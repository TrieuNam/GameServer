package com.SouthMillion.friend_service.service;

import com.SouthMillion.friend_service.client.BagClient;
import com.SouthMillion.friend_service.client.RoleClient;
import com.SouthMillion.friend_service.dto.FriendDTO;
import com.SouthMillion.friend_service.entity.BlockedPlayer;
import com.SouthMillion.friend_service.entity.Friend;
import com.SouthMillion.friend_service.entity.FriendRequest;
import com.SouthMillion.friend_service.entity.OnlineStatus;
import com.SouthMillion.friend_service.repository.BlockedPlayerRepository;
import com.SouthMillion.friend_service.repository.FriendRepository;
import com.SouthMillion.friend_service.repository.FriendRequestRepository;
import com.SouthMillion.friend_service.repository.OnlineStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendService Tests")
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private FriendRequestRepository requestRepository;

    @Mock
    private BlockedPlayerRepository blockedRepository;

    @Mock
    private OnlineStatusRepository statusRepository;

    @Mock
    private BagClient bagClient;

    @Mock
    private RoleClient roleClient;

    @InjectMocks
    private FriendService friendService;

    private static final String ROLE_A = "1001";
    private static final String ROLE_B = "1002";

    // =========================================================
    // getFriendList
    // =========================================================
    @Nested
    @DisplayName("getFriendList()")
    class GetFriendList {

        @Test
        @DisplayName("TC-FRD-001 [P] Lay danh sach ban be – tra ve dung")
        void getFriendList_returnsFriends() {
            Friend f = mock(Friend.class);
            given(f.getOtherRoleId(anyLong())).willReturn(Long.parseLong(ROLE_B));
            given(f.getOtherRoleName(anyLong())).willReturn("Player2");
            given(f.getFriendshipLevel()).willReturn(1);
            given(f.getFriendshipPoints()).willReturn(0L);

            given(friendRepository.findAllFriends(anyLong())).willReturn(List.of(f));
            given(statusRepository.existsByRoleIdAndOnlineTrue(anyLong())).willReturn(false);
            given(statusRepository.findByRoleId(anyLong())).willReturn(Optional.empty());

            FriendDTO.Response<List<FriendDTO.FriendInfo>> resp = friendService.getFriendList(ROLE_A);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).hasSize(1);
            assertThat(resp.getData().get(0).getRoleId()).isEqualTo(ROLE_B);
        }

        @Test
        @DisplayName("TC-FRD-002 [P] Khong co ban be – tra ve danh sach rong")
        void getFriendList_empty_returnsEmptyList() {
            given(friendRepository.findAllFriends(anyLong())).willReturn(List.of());

            FriendDTO.Response<List<FriendDTO.FriendInfo>> resp = friendService.getFriendList(ROLE_A);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).isEmpty();
        }
    }

    // =========================================================
    // sendFriendRequest
    // =========================================================
    @Nested
    @DisplayName("sendFriendRequest()")
    class SendFriendRequest {

        private FriendDTO.AddFriendRequest req(String from, String to) {
            return FriendDTO.AddFriendRequest.builder()
                    .senderId(from)
                    .senderName("PlayerA")
                    .senderLevel(10)
                    .receiverId(to)
                    .receiverName("PlayerB")
                    .build();
        }

        @Test
        @DisplayName("TC-FRD-010 [P] Gui loi moi ket ban thanh cong")
        void sendFriendRequest_success() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(requestRepository.existsBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0))).willReturn(false);
            given(requestRepository.findBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0)))
                    .willReturn(Optional.empty());
            given(friendRepository.countFriends(anyLong())).willReturn(0L);
            given(requestRepository.save(any(FriendRequest.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isZero();
            then(requestRepository).should().save(any(FriendRequest.class));
        }

        @Test
        @DisplayName("TC-FRD-011 [N] Tu ket ban voi chinh minh – tra ve error(-1)")
        void sendFriendRequest_selfAdd_returnsError() {
            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_A));

            assertThat(resp.getCode()).isEqualTo(-1);
            then(requestRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-FRD-012 [N] Da la ban be – tra ve error(-2)")
        void sendFriendRequest_alreadyFriends_returnsError() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(true);

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-2);
        }

        @Test
        @DisplayName("TC-FRD-013 [N] Bi chan boi nguoi nhan – tra ve error(-3)")
        void sendFriendRequest_blockedByReceiver_returnsError() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(true);

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-FRD-014 [N] Da co loi moi chua xu ly – tra ve error(-4)")
        void sendFriendRequest_pendingRequestExists_returnsError() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(requestRepository.existsBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0))).willReturn(true);

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-4);
        }

        @Test
        @DisplayName("TC-FRD-015 [N] Danh sach ban be day (100 ban) – tra ve error(-5)")
        void sendFriendRequest_friendListFull_returnsError() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(requestRepository.existsBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0))).willReturn(false);
            given(requestRepository.findBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0)))
                    .willReturn(Optional.empty());
            given(friendRepository.countFriends(anyLong())).willReturn(100L);

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-5);
        }

        @Test
        @DisplayName("TC-FRD-016 [I] Nguoi nhan da gui loi moi truoc – tu dong chap nhan")
        void sendFriendRequest_reverseRequestExists_autoAccept() {
            FriendRequest reverseReq = mock(FriendRequest.class);
            given(reverseReq.getId()).willReturn(99L);
            given(reverseReq.getSenderId()).willReturn(Long.parseLong(ROLE_B));
            given(reverseReq.getReceiverId()).willReturn(Long.parseLong(ROLE_A));
            given(reverseReq.getSenderName()).willReturn("PlayerB");
            given(reverseReq.getReceiverName()).willReturn("PlayerA");

            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(requestRepository.existsBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0))).willReturn(false);
            given(requestRepository.findBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), eq(0)))
                    .willReturn(Optional.of(reverseReq));

            // For acceptFriendRequest
            given(requestRepository.findById(99L)).willReturn(Optional.of(reverseReq));
            given(friendRepository.countFriends(anyLong())).willReturn(0L);
            given(friendRepository.save(any(Friend.class))).willAnswer(inv -> inv.getArgument(0));
            given(requestRepository.save(any(FriendRequest.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.Response<Void> resp = friendService.sendFriendRequest(req(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isZero();
            then(friendRepository).should().save(any(Friend.class));
        }
    }

    // =========================================================
    // handleFriendRequest
    // =========================================================
    @Nested
    @DisplayName("handleFriendRequest()")
    class HandleFriendRequest {

        @Test
        @DisplayName("TC-FRD-020 [P] Chap nhan loi moi ket ban")
        void handleFriendRequest_approve_success() {
            FriendRequest req = mock(FriendRequest.class);
            given(req.getReceiverId()).willReturn(Long.parseLong(ROLE_B));
            given(req.getSenderId()).willReturn(Long.parseLong(ROLE_A));
            given(req.isPending()).willReturn(true);

            given(requestRepository.findById(1L)).willReturn(Optional.of(req));

            FriendDTO.HandleRequest handleReq = FriendDTO.HandleRequest.builder()
                    .requestId(1L)
                    .receiverId(ROLE_B)
                    .approve(true)
                    .build();

            // acceptFriendRequest calls requestRepository.findById again
            given(friendRepository.countFriends(anyLong())).willReturn(0L);
            given(friendRepository.save(any(Friend.class))).willAnswer(inv -> inv.getArgument(0));
            given(requestRepository.save(any(FriendRequest.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.Response<Void> resp = friendService.handleFriendRequest(handleReq);

            assertThat(resp.getCode()).isZero();
        }

        @Test
        @DisplayName("TC-FRD-021 [P] Tu choi loi moi ket ban")
        void handleFriendRequest_reject_success() {
            FriendRequest req = mock(FriendRequest.class);
            given(req.getReceiverId()).willReturn(Long.parseLong(ROLE_B));
            given(req.isPending()).willReturn(true);

            given(requestRepository.findById(1L)).willReturn(Optional.of(req));
            given(requestRepository.save(any(FriendRequest.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.HandleRequest handleReq = FriendDTO.HandleRequest.builder()
                    .requestId(1L)
                    .receiverId(ROLE_B)
                    .approve(false)
                    .build();

            FriendDTO.Response<Void> resp = friendService.handleFriendRequest(handleReq);

            assertThat(resp.getCode()).isZero();
            then(req).should().reject();
        }

        @Test
        @DisplayName("TC-FRD-022 [N] Loi moi khong ton tai – tra ve error(-1)")
        void handleFriendRequest_notFound_returnsError() {
            given(requestRepository.findById(999L)).willReturn(Optional.empty());

            FriendDTO.HandleRequest handleReq = FriendDTO.HandleRequest.builder()
                    .requestId(999L)
                    .receiverId(ROLE_B)
                    .approve(true)
                    .build();

            FriendDTO.Response<Void> resp = friendService.handleFriendRequest(handleReq);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-FRD-023 [N] Sai nguoi nhan – tra ve error(-2)")
        void handleFriendRequest_wrongReceiver_returnsError() {
            FriendRequest req = mock(FriendRequest.class);
            given(req.getReceiverId()).willReturn(9999L);

            given(requestRepository.findById(1L)).willReturn(Optional.of(req));

            FriendDTO.HandleRequest handleReq = FriendDTO.HandleRequest.builder()
                    .requestId(1L)
                    .receiverId(ROLE_B)
                    .approve(true)
                    .build();

            FriendDTO.Response<Void> resp = friendService.handleFriendRequest(handleReq);

            assertThat(resp.getCode()).isEqualTo(-2);
        }
    }

    // =========================================================
    // removeFriend
    // =========================================================
    @Nested
    @DisplayName("removeFriend()")
    class RemoveFriend {

        @Test
        @DisplayName("TC-FRD-030 [P] Xoa ban be thanh cong")
        void removeFriend_success() {
            Friend friend = new Friend();
            given(friendRepository.findByRoleIds(anyLong(), anyLong())).willReturn(Optional.of(friend));

            FriendDTO.Response<Void> resp = friendService.removeFriend(ROLE_A, ROLE_B);

            assertThat(resp.getCode()).isZero();
            then(friendRepository).should().delete(friend);
        }

        @Test
        @DisplayName("TC-FRD-031 [N] Khong phai ban be – tra ve error(-1)")
        void removeFriend_notFriends_returnsError() {
            given(friendRepository.findByRoleIds(anyLong(), anyLong())).willReturn(Optional.empty());

            FriendDTO.Response<Void> resp = friendService.removeFriend(ROLE_A, ROLE_B);

            assertThat(resp.getCode()).isEqualTo(-1);
            then(friendRepository).should(never()).delete(any());
        }
    }

    // =========================================================
    // blockPlayer
    // =========================================================
    @Nested
    @DisplayName("blockPlayer()")
    class BlockPlayer {

        private FriendDTO.BlockRequest blockReq(String blocker, String blocked) {
            return FriendDTO.BlockRequest.builder()
                    .blockerId(blocker)
                    .blockedId(blocked)
                    .blockedName("Player2")
                    .reason("Spam")
                    .build();
        }

        @Test
        @DisplayName("TC-FRD-040 [P] Chan nguoi choi thanh cong")
        void blockPlayer_success() {
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.countByBlockerId(anyLong())).willReturn(0L);
            given(friendRepository.findByRoleIds(anyLong(), anyLong())).willReturn(Optional.empty());
            given(blockedRepository.save(any(BlockedPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.Response<Void> resp = friendService.blockPlayer(blockReq(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isZero();
            then(blockedRepository).should().save(any(BlockedPlayer.class));
        }

        @Test
        @DisplayName("TC-FRD-041 [N] Tu chan chinh minh – tra ve error(-1)")
        void blockPlayer_selfBlock_returnsError() {
            FriendDTO.Response<Void> resp = friendService.blockPlayer(blockReq(ROLE_A, ROLE_A));

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-FRD-042 [N] Da chan roi – tra ve error(-2)")
        void blockPlayer_alreadyBlocked_returnsError() {
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(true);

            FriendDTO.Response<Void> resp = friendService.blockPlayer(blockReq(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-2);
        }

        @Test
        @DisplayName("TC-FRD-043 [N] Danh sach chan day (50) – tra ve error(-3)")
        void blockPlayer_blockListFull_returnsError() {
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.countByBlockerId(anyLong())).willReturn(50L);

            FriendDTO.Response<Void> resp = friendService.blockPlayer(blockReq(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-FRD-044 [P] Chan nguoi ban – xoa ban be va chan")
        void blockPlayer_existingFriend_removesAndBlocks() {
            Friend friend = new Friend();
            given(blockedRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);
            given(blockedRepository.countByBlockerId(anyLong())).willReturn(0L);
            given(friendRepository.findByRoleIds(anyLong(), anyLong())).willReturn(Optional.of(friend));
            given(blockedRepository.save(any(BlockedPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            FriendDTO.Response<Void> resp = friendService.blockPlayer(blockReq(ROLE_A, ROLE_B));

            assertThat(resp.getCode()).isZero();
            then(friendRepository).should().delete(friend);
        }
    }

    // =========================================================
    // unblockPlayer
    // =========================================================
    @Nested
    @DisplayName("unblockPlayer()")
    class UnblockPlayer {

        @Test
        @DisplayName("TC-FRD-050 [P] Bo chan nguoi choi thanh cong")
        void unblockPlayer_success() {
            BlockedPlayer blocked = new BlockedPlayer();
            given(blockedRepository.findByBlockerIdAndBlockedId(anyLong(), anyLong()))
                    .willReturn(Optional.of(blocked));

            FriendDTO.Response<Void> resp = friendService.unblockPlayer(ROLE_A, ROLE_B);

            assertThat(resp.getCode()).isZero();
            then(blockedRepository).should().delete(blocked);
        }

        @Test
        @DisplayName("TC-FRD-051 [N] Nguoi choi chua bi chan – tra ve error(-1)")
        void unblockPlayer_notBlocked_returnsError() {
            given(blockedRepository.findByBlockerIdAndBlockedId(anyLong(), anyLong()))
                    .willReturn(Optional.empty());

            FriendDTO.Response<Void> resp = friendService.unblockPlayer(ROLE_A, ROLE_B);

            assertThat(resp.getCode()).isEqualTo(-1);
        }
    }

    // =========================================================
    // giveGift
    // =========================================================
    @Nested
    @DisplayName("giveGift()")
    class GiveGift {

        @Test
        @DisplayName("TC-FRD-060 [P] Tang qua ban be – tang diem tinh cam")
        void giveGift_success_addsFriendshipPoints() {
            Friend friend = mock(Friend.class);
            given(friend.getFriendshipLevel()).willReturn(1);
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(true);
            given(friendRepository.findByRoleIds(anyLong(), anyLong())).willReturn(Optional.of(friend));
            given(friendRepository.save(friend)).willReturn(friend);

            FriendDTO.GiveGiftRequest req = FriendDTO.GiveGiftRequest.builder()
                    .giverId(ROLE_A)
                    .receiverId(ROLE_B)
                    .itemId(1001)
                    .quantity(1)
                    .build();

            FriendDTO.Response<Void> resp = friendService.giveGift(req);

            assertThat(resp.getCode()).isZero();
            then(friend).should().addFriendshipPoints(10);
        }

        @Test
        @DisplayName("TC-FRD-061 [N] Tang qua nguoi khong phai ban be – tra ve error(-1)")
        void giveGift_notFriends_returnsError() {
            given(friendRepository.areFriends(anyLong(), anyLong())).willReturn(false);

            FriendDTO.GiveGiftRequest req = FriendDTO.GiveGiftRequest.builder()
                    .giverId(ROLE_A)
                    .receiverId(ROLE_B)
                    .itemId(1001)
                    .quantity(1)
                    .build();

            FriendDTO.Response<Void> resp = friendService.giveGift(req);

            assertThat(resp.getCode()).isEqualTo(-1);
        }
    }
}
