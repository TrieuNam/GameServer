package com.SouthMillion.guild_service.service;

import com.SouthMillion.guild_service.client.RoleClient;
import com.SouthMillion.guild_service.client.WalletClient;
import com.SouthMillion.guild_service.dto.GuildDTO;
import com.SouthMillion.guild_service.entity.Guild;
import com.SouthMillion.guild_service.entity.GuildApplication;
import com.SouthMillion.guild_service.entity.GuildMember;
import com.SouthMillion.guild_service.repository.GuildApplicationRepository;
import com.SouthMillion.guild_service.repository.GuildMemberRepository;
import com.SouthMillion.guild_service.repository.GuildRepository;
import com.SouthMillion.guild_service.repository.GuildWarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Guild Service
 * 
 * Core business logic for guild management system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuildService {

    private final GuildRepository guildRepository;
    private final GuildMemberRepository memberRepository;
    private final GuildApplicationRepository applicationRepository;
    private final GuildWarehouseRepository warehouseRepository;
    private final WalletClient walletClient;
    private final RoleClient roleClient;
    private final GuildEventPublisher guildEventPublisher;

    private static final long CREATE_GUILD_COST = 10000L; // 10,000 gold
    private static final int MAX_GUILD_LEVEL = 10;
    private static final int MAX_MEMBERS = 50;
    private static final int MAX_WAREHOUSE_SLOTS = 100;

    /** Parse numeric String roleId to Long */
    private static Long toId(String s) { return Long.parseLong(s); }

    /**
     * Create new guild
     */
    @Transactional
    public GuildDTO.Response<GuildDTO.InfoResponse> createGuild(GuildDTO.CreateRequest request) {
        log.info("Creating guild: name={}, leaderId={}", request.getName(), request.getLeaderId());

        // Check if name exists
        if (guildRepository.existsByNameIgnoreCase(request.getName())) {
            return GuildDTO.Response.error(-1, "Guild name already exists");
        }

        Long leaderIdL = toId(request.getLeaderId());

        // Check if player already in guild
        if (memberRepository.existsByRoleId(leaderIdL)) {
            return GuildDTO.Response.error(-2, "Player already in a guild");
        }

        // Deduct gold from player
        try {
            List<WalletDTOs.Change> changes = new ArrayList<>();
            changes.add(WalletDTOs.Change.builder()
                    .itemId(1L)
                    .amount(CREATE_GUILD_COST)
                    .build());
            WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                    .roleId(request.getLeaderId())
                    .changes(changes)
                    .reason(101)
                    .build();
            walletClient.consumeCurrency(request.getLeaderId(), walletReq);
            log.info("Gold deducted: playerId={}, amount={}", request.getLeaderId(), CREATE_GUILD_COST);
        } catch (Exception e) {
            log.error("Failed to deduct gold: {}", e.getMessage());
            return GuildDTO.Response.error(-3, "Insufficient gold (require 10,000 gold)");
        }

        // Create guild
        Guild guild = Guild.builder()
                .name(request.getName())
                .leaderId(leaderIdL)
                .level(1).exp(0L).memberCount(1).maxMembers(MAX_MEMBERS)
                .notice(request.getNotice())
                .techAttack(1).techDefense(1).techHp(1).techCrit(1).techSpeed(1)
                .funds(0L).active(true)
                .build();

        guild = guildRepository.save(guild);
        log.info("Guild created: id={}, name={}", guild.getId(), guild.getName());

        // Fetch leader role info
        String leaderName = "Leader";
        Integer leaderLevel = 1;
        Long leaderPower = 0L;
        try {
            Optional<RoleDTOs.RoleResp> roleOpt = roleClient.getRoleInfo(leaderIdL);
            if (roleOpt.isPresent()) {
                RoleDTOs.RoleResp roleInfo = roleOpt.get();
                leaderName = roleInfo.getName() != null ? roleInfo.getName() :
                             (roleInfo.getNickname() != null ? roleInfo.getNickname() : "Leader");
                leaderLevel = roleInfo.getLevel() != null ? roleInfo.getLevel() : 1;
                leaderPower = roleInfo.getCap() != null ? roleInfo.getCap() : 0L;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch leader role info: {}", e.getMessage());
        }

        // Add leader as member
        GuildMember leader = GuildMember.builder()
                .guildId(guild.getId())
                .roleId(leaderIdL)
                .roleName(leaderName).roleLevel(leaderLevel).power(leaderPower)
                .rank(3).contribution(0L).online(true)
                .build();

        memberRepository.save(leader);
        log.info("Guild leader added: guildId={}, leaderId={}", guild.getId(), request.getLeaderId());

        // Publish Kafka events: guild.created + guild.joined (for leader)
        guildEventPublisher.publishGuildCreated(leaderIdL, guild.getId(), guild.getName());

        return GuildDTO.Response.success(buildGuildInfo(guild, leader.getRoleName()));
    }

    /**
     * Get guild info
     */
    @Transactional(readOnly = true)
    public GuildDTO.Response<GuildDTO.InfoResponse> getGuildInfo(Long guildId) {
        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Guild not found");
        }

        Guild guild = guildOpt.get();
        if (!guild.getActive()) {
            return GuildDTO.Response.error(-2, "Guild is disbanded");
        }

        // Get leader info
        Optional<GuildMember> leaderOpt = memberRepository.findByGuildIdAndRank(guildId, 3);
        String leaderName = leaderOpt.map(GuildMember::getRoleName).orElse("Unknown");

        return GuildDTO.Response.success(buildGuildInfo(guild, leaderName));
    }

    /**
     * Compatibility endpoint data for websocket-server: GET /api/guild/player/{roleId}
     * Returns empty map when player is not in a guild.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlayerGuildCompat(String roleId) {
        Map<String, Object> out = new HashMap<>();

        Long roleIdLong;
        try {
            roleIdLong = toId(roleId);
        } catch (Exception ex) {
            return out;
        }

        Optional<GuildMember> memberOpt = memberRepository.findByRoleId(roleIdLong);
        if (memberOpt.isEmpty()) {
            return out;
        }

        GuildMember member = memberOpt.get();
        Optional<Guild> guildOpt = guildRepository.findById(member.getGuildId());
        if (guildOpt.isEmpty() || !Boolean.TRUE.equals(guildOpt.get().getActive())) {
            return out;
        }

        Guild guild = guildOpt.get();
        out.put("guildId", guild.getId());
        out.put("name", guild.getName());
        out.put("memberCount", guild.getMemberCount());
        out.put("myPosition", member.getRank());
        return out;
    }

    /**
     * Search guilds
     */
    @Transactional(readOnly = true)
    public GuildDTO.Response<GuildDTO.PageResponse<GuildDTO.ListItem>> searchGuilds(GuildDTO.SearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        
        Page<Guild> page;
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            page = guildRepository.searchByName(request.getKeyword().trim(), pageable);
        } else {
            page = guildRepository.findTopGuilds(pageable);
        }

        List<GuildDTO.ListItem> items = page.getContent().stream()
                .map(this::buildGuildListItem)
                .collect(Collectors.toList());

        GuildDTO.PageResponse<GuildDTO.ListItem> pageResponse = GuildDTO.PageResponse.<GuildDTO.ListItem>builder()
                .content(items)
                .page(request.getPage())
                .size(request.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return GuildDTO.Response.success(pageResponse);
    }

    /**
     * Apply to join guild
     */
    @Transactional
    public GuildDTO.Response<Void> applyToGuild(GuildDTO.JoinRequest request) {
        log.info("Player applying to guild: roleId={}, guildId={}", request.getRoleId(), request.getGuildId());

        // Check if guild exists
        Optional<Guild> guildOpt = guildRepository.findById(request.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Guild not found");
        }

        Guild guild = guildOpt.get();
        if (!guild.getActive()) {
            return GuildDTO.Response.error(-2, "Guild is disbanded");
        }

        // Check if guild is full
        if (guild.isFull()) {
            return GuildDTO.Response.error(-3, "Guild is full");
        }

        // Check if player already in a guild
        if (memberRepository.existsByRoleId(toId(request.getRoleId()))) {
            return GuildDTO.Response.error(-4, "Player already in a guild");
        }

        // Check if already has pending application
        if (applicationRepository.existsByGuildIdAndRoleIdAndStatus(request.getGuildId(), toId(request.getRoleId()), 0)) {
            return GuildDTO.Response.error(-5, "Application already pending");
        }

        // Create application
        GuildApplication application = GuildApplication.builder()
                .guildId(request.getGuildId())
                .roleId(toId(request.getRoleId()))
                .roleName(request.getRoleName())
                .roleLevel(request.getRoleLevel())
                .power(request.getPower())
                .message(request.getMessage())
                .status(0) // Pending
                .build();

        applicationRepository.save(application);
        log.info("Guild application created: id={}, guildId={}, roleId={}", 
                 application.getId(), request.getGuildId(), request.getRoleId());

        return GuildDTO.Response.success(null);
    }

    /**
     * Process guild application (approve/reject)
     */
    @Transactional
    public GuildDTO.Response<Void> processApplication(GuildDTO.ProcessApplicationRequest request) {
        log.info("Processing application: id={}, processorId={}, approve={}", 
                 request.getApplicationId(), request.getProcessorId(), request.getApprove());

        // Find application
        Optional<GuildApplication> appOpt = applicationRepository.findById(request.getApplicationId());
        if (appOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Application not found");
        }

        GuildApplication application = appOpt.get();
        if (!application.isPending()) {
            return GuildDTO.Response.error(-2, "Application already processed");
        }

        // Check if processor has permission (must be officer or leader)
        Optional<GuildMember> processorOpt = memberRepository.findByGuildIdAndRoleId(
                application.getGuildId(), toId(request.getProcessorId()));
        if (processorOpt.isEmpty() || !processorOpt.get().isOfficerOrAbove()) {
            return GuildDTO.Response.error(-3, "No permission to process applications");
        }

        // Get guild
        Optional<Guild> guildOpt = guildRepository.findById(application.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-4, "Guild not found");
        }

        Guild guild = guildOpt.get();

        if (request.getApprove()) {
            // Approve - add member to guild
            if (guild.isFull()) {
                return GuildDTO.Response.error(-5, "Guild is full");
            }

            // Check if player already in another guild
            if (memberRepository.existsByRoleId(application.getRoleId())) {
                return GuildDTO.Response.error(-6, "Player already in a guild");
            }

            // Add member
            GuildMember member = GuildMember.builder()
                    .guildId(guild.getId())
                    .roleId(application.getRoleId())
                    .roleName(application.getRoleName())
                    .roleLevel(application.getRoleLevel())
                    .power(application.getPower())
                    .rank(1) // Regular member
                    .contribution(0L)
                    .online(false)
                    .build();

            memberRepository.save(member);

            // Update guild member count
            guild.setMemberCount(guild.getMemberCount() + 1);
            guildRepository.save(guild);

            application.approve(request.getProcessorId());
            log.info("Guild application approved: id={}, guildId={}, roleId={}", 
                     application.getId(), guild.getId(), application.getRoleId());

            // Publish guild.joined event
            guildEventPublisher.publishGuildJoined(application.getRoleId(), guild.getId(), guild.getName());
        } else {
            // Reject
            application.reject(request.getProcessorId());
            log.info("Guild application rejected: id={}", application.getId());
        }

        applicationRepository.save(application);
        return GuildDTO.Response.success(null);
    }

    /**
     * Leave guild
     */
    @Transactional
    public GuildDTO.Response<Void> leaveGuild(Long guildId, String roleId) {
        log.info("Player leaving guild: roleId={}, guildId={}", roleId, guildId);

        // Find member
        Optional<GuildMember> memberOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(roleId));
        if (memberOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Player not in this guild");
        }

        GuildMember member = memberOpt.get();

        // Leader cannot leave (must transfer or disband)
        if (member.isLeader()) {
            return GuildDTO.Response.error(-2, "Leader cannot leave guild. Transfer leadership or disband guild.");
        }

        // Remove member
        memberRepository.delete(member);

        // Update guild member count
        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isPresent()) {
            Guild guild = guildOpt.get();
            guild.setMemberCount(Math.max(0, guild.getMemberCount() - 1));
            guildRepository.save(guild);
        }

        log.info("Player left guild: roleId={}, guildId={}", roleId, guildId);

        // Publish guild.left event
        String guildName = guildRepository.findById(guildId).map(g -> g.getName()).orElse("");
        guildEventPublisher.publishGuildLeft(toId(roleId), guildId, guildName, "LEFT");

        return GuildDTO.Response.success(null);
    }

    /**
     * Kick member from guild
     */
    @Transactional
    public GuildDTO.Response<Void> kickMember(Long guildId, String kickerId, String targetId) {
        log.info("Kicking member: guildId={}, kickerId={}, targetId={}", guildId, kickerId, targetId);

        // Check kicker permission
        Optional<GuildMember> kickerOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(kickerId));
        if (kickerOpt.isEmpty() || !kickerOpt.get().isOfficerOrAbove()) {
            return GuildDTO.Response.error(-1, "No permission to kick members");
        }

        // Find target
        Optional<GuildMember> targetOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(targetId));
        if (targetOpt.isEmpty()) {
            return GuildDTO.Response.error(-2, "Target player not in guild");
        }

        GuildMember target = targetOpt.get();

        // Cannot kick leader
        if (target.isLeader()) {
            return GuildDTO.Response.error(-3, "Cannot kick guild leader");
        }

        // Officers can only kick regular members
        GuildMember kicker = kickerOpt.get();
        if (kicker.getRank() == 2 && target.getRank() >= 2) {
            return GuildDTO.Response.error(-4, "Officers can only kick regular members");
        }

        // Remove member
        memberRepository.delete(target);

        // Update guild member count
        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isPresent()) {
            Guild guild = guildOpt.get();
            guild.setMemberCount(Math.max(0, guild.getMemberCount() - 1));
            guildRepository.save(guild);
        }

        log.info("Member kicked: targetId={}, guildId={}", targetId, guildId);

        // Publish guild.left event
        String guildName = guildRepository.findById(guildId).map(g -> g.getName()).orElse("");
        guildEventPublisher.publishGuildLeft(toId(targetId), guildId, guildName, "KICKED");

        return GuildDTO.Response.success(null);
    }

    /**
     * Promote member
     */
    @Transactional
    public GuildDTO.Response<Void> promoteMember(Long guildId, String promoterId, String targetId) {
        log.info("Promoting member: guildId={}, promoterId={}, targetId={}", guildId, promoterId, targetId);

        // Only leader can promote
        Optional<GuildMember> promoterOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(promoterId));
        if (promoterOpt.isEmpty() || !promoterOpt.get().isLeader()) {
            return GuildDTO.Response.error(-1, "Only leader can promote members");
        }

        // Find target
        Optional<GuildMember> targetOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(targetId));
        if (targetOpt.isEmpty()) {
            return GuildDTO.Response.error(-2, "Target player not in guild");
        }

        GuildMember target = targetOpt.get();
        if (!target.promote()) {
            return GuildDTO.Response.error(-3, "Cannot promote further");
        }

        memberRepository.save(target);
        log.info("Member promoted: targetId={}, newRank={}", targetId, target.getRank());
        return GuildDTO.Response.success(null);
    }

    /**
     * Demote member
     */
    @Transactional
    public GuildDTO.Response<Void> demoteMember(Long guildId, String demoterId, String targetId) {
        log.info("Demoting member: guildId={}, demoterId={}, targetId={}", guildId, demoterId, targetId);

        // Only leader can demote
        Optional<GuildMember> demoterOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(demoterId));
        if (demoterOpt.isEmpty() || !demoterOpt.get().isLeader()) {
            return GuildDTO.Response.error(-1, "Only leader can demote members");
        }

        // Find target
        Optional<GuildMember> targetOpt = memberRepository.findByGuildIdAndRoleId(guildId, toId(targetId));
        if (targetOpt.isEmpty()) {
            return GuildDTO.Response.error(-2, "Target player not in guild");
        }

        GuildMember target = targetOpt.get();
        if (target.isLeader()) {
            return GuildDTO.Response.error(-3, "Cannot demote guild leader");
        }

        if (!target.demote()) {
            return GuildDTO.Response.error(-4, "Cannot demote further");
        }

        memberRepository.save(target);
        log.info("Member demoted: targetId={}, newRank={}", targetId, target.getRank());
        return GuildDTO.Response.success(null);
    }

    /**
     * Transfer leadership
     */
    @Transactional
    public GuildDTO.Response<Void> transferLeadership(GuildDTO.TransferLeaderRequest request) {
        log.info("Transferring leadership: guildId={}, from={}, to={}", 
                 request.getGuildId(), request.getCurrentLeaderId(), request.getNewLeaderId());

        // Find guild
        Optional<Guild> guildOpt = guildRepository.findById(request.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Guild not found");
        }

        Guild guild = guildOpt.get();

        // Verify current leader
        if (!guild.getLeaderId().equals(toId(request.getCurrentLeaderId()))) {
            return GuildDTO.Response.error(-2, "Not the guild leader");
        }

        // Find current leader member
        Optional<GuildMember> currentLeaderOpt = memberRepository.findByGuildIdAndRoleId(
                request.getGuildId(), toId(request.getCurrentLeaderId()));
        if (currentLeaderOpt.isEmpty()) {
            return GuildDTO.Response.error(-3, "Current leader not found");
        }

        // Find new leader member
        Optional<GuildMember> newLeaderOpt = memberRepository.findByGuildIdAndRoleId(
                request.getGuildId(), toId(request.getNewLeaderId()));
        if (newLeaderOpt.isEmpty()) {
            return GuildDTO.Response.error(-4, "New leader not in guild");
        }

        // Transfer
        GuildMember currentLeader = currentLeaderOpt.get();
        GuildMember newLeader = newLeaderOpt.get();

        currentLeader.setRank(2); // Demote to officer
        newLeader.setRank(3); // Promote to leader

        memberRepository.save(currentLeader);
        memberRepository.save(newLeader);

        guild.setLeaderId(toId(request.getNewLeaderId()));
        guildRepository.save(guild);

        log.info("Leadership transferred: guildId={}, newLeaderId={}", request.getGuildId(), request.getNewLeaderId());
        return GuildDTO.Response.success(null);
    }

    /**
     * Disband guild
     */
    @Transactional
    public GuildDTO.Response<Void> disbandGuild(Long guildId, String leaderId) {
        log.info("Disbanding guild: guildId={}, leaderId={}", guildId, leaderId);

        // Find guild
        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Guild not found");
        }

        Guild guild = guildOpt.get();

        // Verify leader
        if (!guild.getLeaderId().equals(toId(leaderId))) {
            return GuildDTO.Response.error(-2, "Only leader can disband guild");
        }

        // Mark as disbanded
        guild.setActive(false);
        guild.setDisbandedAt(LocalDateTime.now());
        guildRepository.save(guild);

        // Publish guild.left for all members before deleting them
        List<com.SouthMillion.guild_service.entity.GuildMember> allMembers =
                memberRepository.findByGuildIdOrderByRankDescContributionDesc(guildId);
        for (com.SouthMillion.guild_service.entity.GuildMember m : allMembers) {
            guildEventPublisher.publishGuildLeft(m.getRoleId(), guildId, guild.getName(), "DISBANDED");
        }

        // Delete all members
        memberRepository.deleteByGuildId(guildId);

        // Delete all applications
        applicationRepository.deleteByGuildId(guildId);

        // Delete warehouse items
        warehouseRepository.deleteByGuildId(guildId);

        log.info("Guild disbanded: guildId={}", guildId);
        return GuildDTO.Response.success(null);
    }

    /**
     * Donate to guild
     */
    @Transactional
    public GuildDTO.Response<Void> donate(GuildDTO.DonateRequest request) {
        log.info("Player donating to guild: roleId={}, guildId={}, amount={}", 
                 request.getRoleId(), request.getGuildId(), request.getAmount());

        // Find member
        Optional<GuildMember> memberOpt = memberRepository.findByGuildIdAndRoleId(
                request.getGuildId(), toId(request.getRoleId()));
        if (memberOpt.isEmpty()) {
            return GuildDTO.Response.error(-1, "Player not in guild");
        }

        GuildMember member = memberOpt.get();

        // Check daily donation limit
        if (!member.canDonateToday()) {
            return GuildDTO.Response.error(-2, "Daily donation limit reached (max 3 per day)");
        }

        // Deduct gold from player
        try {
            List<WalletDTOs.Change> changes = new ArrayList<>();
            changes.add(WalletDTOs.Change.builder()
                    .itemId(1L) // Gold
                    .amount(request.getAmount())
                    .build());
            
            WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                    .roleId(request.getRoleId())
                    .changes(changes)
                    .reason(102) // GUILD_DONATE
                    .build();
            walletClient.consumeCurrency(request.getRoleId(), walletReq);
            log.info("Gold donated: playerId={}, amount={}", request.getRoleId(), request.getAmount());
        } catch (Exception e) {
            log.error("Failed to donate gold: {}", e.getMessage());
            return GuildDTO.Response.error(-3, "Insufficient gold");
        }

        // Find guild
        Optional<Guild> guildOpt = guildRepository.findById(request.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-3, "Guild not found");
        }

        Guild guild = guildOpt.get();

        // Add funds to guild
        guild.addFunds(request.getAmount());
        guildRepository.save(guild);

        // Add contribution to member (1 gold = 1 contribution point)
        long contribution = request.getAmount();
        member.addContribution(contribution);
        member.donate();
        memberRepository.save(member);

        // Add exp to guild (1 gold = 1 exp)
        boolean leveledUp = guild.addExp(request.getAmount());
        if (leveledUp) {
            log.info("Guild leveled up! guildId={}, newLevel={}", guild.getId(), guild.getLevel());
        }
        guildRepository.save(guild);

        log.info("Donation successful: roleId={}, guildId={}, amount={}, contribution={}", 
                 request.getRoleId(), request.getGuildId(), request.getAmount(), contribution);
        return GuildDTO.Response.success(null);
    }

    /**
     * Upgrade guild technology
     */
    @Transactional
    public GuildDTO.Response<Void> upgradeTech(GuildDTO.UpgradeTechRequest request) {
        log.info("Upgrading guild tech: guildId={}, roleId={}, techType={}", 
                 request.getGuildId(), request.getRoleId(), request.getTechType());

        // Check permission (officer or leader)
        Optional<GuildMember> memberOpt = memberRepository.findByGuildIdAndRoleId(
                request.getGuildId(), toId(request.getRoleId()));
        if (memberOpt.isEmpty() || !memberOpt.get().isOfficerOrAbove()) {
            return GuildDTO.Response.error(-1, "No permission to upgrade technology");
        }

        // Find guild
        Optional<Guild> guildOpt = guildRepository.findById(request.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-2, "Guild not found");
        }

        Guild guild = guildOpt.get();

        // Upgrade tech
        if (!guild.upgradeTech(request.getTechType())) {
            long cost = guild.getTechUpgradeCost(request.getTechType());
            return GuildDTO.Response.error(-3, 
                String.format("Cannot upgrade tech. Cost: %d, Available: %d", cost, guild.getFunds()));
        }

        guildRepository.save(guild);
        log.info("Guild tech upgraded: guildId={}, techType={}, newLevel={}", 
                 request.getGuildId(), request.getTechType(), guild.getTechLevel(request.getTechType()));
        return GuildDTO.Response.success(null);
    }

    /**
     * Edit guild notice
     */
    @Transactional
    public GuildDTO.Response<Void> editNotice(GuildDTO.EditNoticeRequest request) {
        log.info("Editing guild notice: guildId={}, roleId={}", request.getGuildId(), request.getRoleId());

        // Check permission (officer or leader)
        Optional<GuildMember> memberOpt = memberRepository.findByGuildIdAndRoleId(
                request.getGuildId(), toId(request.getRoleId()));
        if (memberOpt.isEmpty() || !memberOpt.get().isOfficerOrAbove()) {
            return GuildDTO.Response.error(-1, "No permission to edit notice");
        }

        // Find guild
        Optional<Guild> guildOpt = guildRepository.findById(request.getGuildId());
        if (guildOpt.isEmpty()) {
            return GuildDTO.Response.error(-2, "Guild not found");
        }

        Guild guild = guildOpt.get();
        guild.setNotice(request.getNotice());
        guildRepository.save(guild);

        log.info("Guild notice updated: guildId={}", request.getGuildId());
        return GuildDTO.Response.success(null);
    }

    /**
     * Get guild members
     */
    @Transactional(readOnly = true)
    public GuildDTO.Response<List<GuildDTO.MemberInfo>> getMembers(Long guildId) {
        List<GuildMember> members = memberRepository.findByGuildIdOrderByRankDescContributionDesc(guildId);
        
        List<GuildDTO.MemberInfo> memberInfos = members.stream()
                .map(this::buildMemberInfo)
                .collect(Collectors.toList());

        return GuildDTO.Response.success(memberInfos);
    }

    /**
     * Get guild applications
     */
    @Transactional(readOnly = true)
    public GuildDTO.Response<List<GuildDTO.ApplicationInfo>> getApplications(Long guildId) {
        List<GuildApplication> applications = applicationRepository.findByGuildIdAndStatusOrderByAppliedAtDesc(guildId, 0);
        
        List<GuildDTO.ApplicationInfo> appInfos = applications.stream()
                .map(this::buildApplicationInfo)
                .collect(Collectors.toList());

        return GuildDTO.Response.success(appInfos);
    }

    // ==================== Helper Methods ====================

    private GuildDTO.InfoResponse buildGuildInfo(Guild guild, String leaderName) {
        Long rank = guildRepository.getGuildRank(guild.getLevel(), guild.getExp());
        
        return GuildDTO.InfoResponse.builder()
                .id(guild.getId())
                .name(guild.getName())
                .leaderId(String.valueOf(guild.getLeaderId()))
                .leaderName(leaderName)
                .level(guild.getLevel())
                .exp(guild.getExp())
                .expForNextLevel(guild.getExpForNextLevel())
                .memberCount(guild.getMemberCount())
                .maxMembers(guild.getMaxMembers())
                .notice(guild.getNotice())
                .funds(guild.getFunds())
                .techAttack(guild.getTechAttack())
                .techDefense(guild.getTechDefense())
                .techHp(guild.getTechHp())
                .techCrit(guild.getTechCrit())
                .techSpeed(guild.getTechSpeed())
                .createdAt(guild.getCreatedAt())
                .updatedAt(guild.getUpdatedAt())
                .rank(rank)
                .build();
    }

    private GuildDTO.ListItem buildGuildListItem(Guild guild) {
        // Calculate total guild power (sum of all members)
        List<GuildMember> members = memberRepository.findByGuildIdOrderByRankDescContributionDesc(guild.getId());
        long totalPower = members.stream().mapToLong(GuildMember::getPower).sum();
        
        return GuildDTO.ListItem.builder()
                .id(guild.getId())
                .name(guild.getName())
                .leaderName(members.stream()
                        .filter(GuildMember::isLeader)
                        .findFirst()
                        .map(GuildMember::getRoleName)
                        .orElse("Unknown"))
                .level(guild.getLevel())
                .memberCount(guild.getMemberCount())
                .maxMembers(guild.getMaxMembers())
                .power(totalPower)
                .recruiting(!guild.isFull())
                .build();
    }

    private GuildDTO.MemberInfo buildMemberInfo(GuildMember member) {
        String rankName = switch (member.getRank()) {
            case 3 -> "Leader";
            case 2 -> "Officer";
            default -> "Member";
        };
        
        return GuildDTO.MemberInfo.builder()
                .id(member.getId())
                .roleId(String.valueOf(member.getRoleId()))
                .roleName(member.getRoleName())
                .roleLevel(member.getRoleLevel())
                .power(member.getPower())
                .rank(member.getRank())
                .rankName(rankName)
                .contribution(member.getContribution())
                .online(member.getOnline())
                .lastOnlineTime(member.getLastOnlineTime())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private GuildDTO.ApplicationInfo buildApplicationInfo(GuildApplication app) {
        return GuildDTO.ApplicationInfo.builder()
                .id(app.getId())
                .guildId(app.getGuildId())
                .roleId(String.valueOf(app.getRoleId()))
                .roleName(app.getRoleName())
                .roleLevel(app.getRoleLevel())
                .power(app.getPower())
                .message(app.getMessage())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .build();
    }
}
