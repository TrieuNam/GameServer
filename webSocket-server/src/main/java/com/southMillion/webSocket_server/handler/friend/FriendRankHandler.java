package com.SouthMillion.webSocket_server.handler.friend;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.FriendFeign;
import com.SouthMillion.webSocket_server.service.client.LeaderboardFeign;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgfriend.Msgfriend;
import org.SouthMillion.proto.Msgrole.Msgrole;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Friend Rank Handler - Friend Ranking System
 *
 * Messages:
 * - C→S 1906 (CS_FRIEND_RANK_REQ) - Client friend ranking request
 * - S→C 1907 (SC_FRIEND_RANK_LIST) - Friend ranking list response
 *
 * This handler retrieves the ranking information for all of a player's friends
 * and sends back a sorted list of friend rankings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRankHandler implements MessageHandler {

    private final FriendFeign friendFeign;
    private final LeaderboardFeign leaderboardFeign;

    private static final int RANK_POWER = 1;
    private static final int RANK_LEVEL = 2;
    private static final int RANK_ARENA = 3;
    private static final int RANK_GUILD = 4;
    private static final int RANK_PET = 5;
    private static final int RANK_DEFAULT = RANK_LEVEL; // Default to level
    private static final int DEFAULT_LIMIT = 10;

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_FRIEND_RANK_REQ}; // 1906
    }

    public String getModuleName() {
        return "friendRank";
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                if (msgId != MsgIds.CS_FRIEND_RANK_REQ) {
                    log.warn("[friendRank] Unknown msgId: {}", msgId);
                    return;
                }

                Msgfriend.GetFriendRankRequest req = Msgfriend.GetFriendRankRequest.parseFrom(payload);
                String roleId = ps.getRoleId() != null ? String.valueOf(ps.getRoleId()) : req.getRoleId();
                int rankingType = req.getRankingType() > 0 ? req.getRankingType() : RANK_DEFAULT;
                int limit = req.getLimit() > 0 ? req.getLimit() : DEFAULT_LIMIT;

                log.debug("[friendRank] roleId={}, rankingType={}, limit={}", roleId, rankingType, limit);

                // Get friend list
                Map<String, Object> friendListResp = friendFeign.getFriendList(roleId);
                List<Map<String, Object>> friendsList = extractFriendsList(friendListResp);

                if (friendsList == null || friendsList.isEmpty()) {
                    log.debug("[friendRank] No friends found for roleId={}", roleId);
                    sendEmptyResponse(ps);
                    return;
                }

                // Extract friend IDs
                Set<String> friendIds = friendsList.stream()
                        .map(f -> String.valueOf(f.get("roleId")))
                        .collect(Collectors.toSet());

                // Query rankings for each friend
                List<FriendRankData> friendRankings = new ArrayList<>();
                for (Map<String, Object> friend : friendsList) {
                    String friendId = String.valueOf(friend.get("roleId"));
                    try {
                        Map<String, Object> rankResp = leaderboardFeign.getLeaderboard(rankingType, friendId);
                        FriendRankData rankData = extractFriendRankData(rankResp, friend, rankingType);
                        if (rankData != null) {
                            friendRankings.add(rankData);
                        }
                    } catch (Exception e) {
                        log.warn("[friendRank] Failed to get rank for friendId={}", friendId, e);
                    }
                }

                // Sort by score descending, then by rank
                friendRankings.sort((a, b) -> {
                    int cmp = Long.compare(b.getScore(), a.getScore());
                    return cmp != 0 ? cmp : Integer.compare(a.getRank(), b.getRank());
                });

                // Limit results
                List<FriendRankData> topFriends = friendRankings.stream()
                        .limit(limit)
                        .collect(Collectors.toList());

                // Get player's own rank
                Map<String, Object> myRankResp = leaderboardFeign.getLeaderboard(rankingType, roleId);
                FriendRankData myRank = extractFriendRankData(myRankResp, null, rankingType);

                // Build response
                Msgfriend.GetFriendRankResponse.Builder responseBuilder = Msgfriend.GetFriendRankResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Friend rankings retrieved successfully");

                for (FriendRankData rankData : topFriends) {
                    Msgfriend.FriendRankEntry.Builder entryBuilder = Msgfriend.FriendRankEntry.newBuilder()
                            .setRank(rankData.getRank())
                            .setFriendId(rankData.getFriendId())
                            .setFriendName(rankData.getFriendName())
                            .setLevel(rankData.getLevel())
                            .setScore(rankData.getScore())
                            .setOnline(rankData.isOnline());
                    if (rankData.getGuildName() != null) {
                        entryBuilder.setGuildName(rankData.getGuildName());
                    }
                    responseBuilder.addRankings(entryBuilder);
                }

                if (myRank != null) {
                    Msgfriend.FriendRankEntry.Builder myRankBuilder = Msgfriend.FriendRankEntry.newBuilder()
                            .setRank(myRank.getRank())
                            .setFriendId(myRank.getFriendId())
                            .setFriendName(myRank.getFriendName())
                            .setLevel(myRank.getLevel())
                            .setScore(myRank.getScore())
                            .setOnline(myRank.isOnline());
                    if (myRank.getGuildName() != null) {
                        myRankBuilder.setGuildName(myRank.getGuildName());
                    }
                    responseBuilder.setMyRank(myRankBuilder);
                }

                Emitters.emit(ps, MsgIds.SC_FRIEND_RANK_LIST, responseBuilder.build().toByteArray());

                log.info("[friendRank] Friend rankings sent - roleId={}, rankingType={}, count={}", 
                        roleId, rankingType, topFriends.size());

            } catch (Exception e) {
                log.error("[friendRank] Error handling friend rank request", e);
                sendEmptyResponse(ps);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFriendsList(Map<String, Object> resp) {
        if (resp == null) return null;
        Object data = resp.get("data");
        if (data instanceof List<?>list) {
            return (List<Map<String, Object>>) list;
        }
        if (data instanceof Map<?, ?> dataMap) {
            Object friends = dataMap.get("friends");
            if (friends instanceof List<?> friendsList) {
                return (List<Map<String, Object>>) friendsList;
            }
        }
        return null;
    }

    private FriendRankData extractFriendRankData(Map<String, Object> rankResp, Map<String, Object> friendInfo, int rankingType) {
        if (rankResp == null) return null;

        FriendRankData data = new FriendRankData();

        // Extract from rank response
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) rankResp.get("data");
            if (responseData != null) {
                // Get myRank from response
                int myRank = getInt(responseData, "myRank", 0);
                long myValue = getLong(responseData, "myValue", 0L);

                data.setRank(myRank);
                data.setScore(myValue);

                // Get role info
                @SuppressWarnings("unchecked")
                Map<String, Object> roleInfo = (Map<String, Object>) responseData.get("roleInfo");
                if (roleInfo == null && friendInfo != null) {
                    roleInfo = friendInfo;
                }

                if (roleInfo != null) {
                    data.setFriendId(String.valueOf(roleInfo.get("roleId")));
                    data.setFriendName(String.valueOf(roleInfo.get("roleName")));
                    data.setLevel(getInt(roleInfo, "level", 0));
                    Object onlineObj = roleInfo.get("online");
                    data.setOnline(onlineObj instanceof Boolean ? (Boolean) onlineObj : false);
                    Object guildName = roleInfo.get("guildName");
                    if (guildName != null) {
                        data.setGuildName(guildName.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[friendRank] Failed to extract rank data: {}", e.getMessage());
        }

        return data;
    }

    private void sendEmptyResponse(PlayerSession ps) {
        try {
            Msgfriend.GetFriendRankResponse empty = Msgfriend.GetFriendRankResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("No friend rankings available")
                    .build();
            Emitters.emit(ps, MsgIds.SC_FRIEND_RANK_LIST, empty.toByteArray());
        } catch (Exception e) {
            log.error("[friendRank] sendEmptyResponse failed", e);
        }
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private long getLong(Map<String, Object> map, String key, long def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.longValue() : def;
    }

    /**
     * Internal DTO for friend ranking data
     */
    private static class FriendRankData {
        private int rank;
        private String friendId;
        private String friendName;
        private int level;
        private long score;
        private boolean online;
        private String guildName;

        // Getters and setters
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public String getFriendId() { return friendId; }
        public void setFriendId(String friendId) { this.friendId = friendId; }

        public String getFriendName() { return friendName; }
        public void setFriendName(String friendName) { this.friendName = friendName; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public long getScore() { return score; }
        public void setScore(long score) { this.score = score; }

        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }

        public String getGuildName() { return guildName; }
        public void setGuildName(String guildName) { this.guildName = guildName; }
    }
}
