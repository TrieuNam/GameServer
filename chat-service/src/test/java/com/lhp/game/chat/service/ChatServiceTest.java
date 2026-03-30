package com.lhp.game.chat.service;

import com.SouthMillion.chat_service.dto.ChatDTO;
import com.SouthMillion.chat_service.entity.ChatMessage;
import com.SouthMillion.chat_service.entity.MutedPlayer;
import com.SouthMillion.chat_service.repository.ChatMessageRepository;
import com.SouthMillion.chat_service.repository.MutedPlayerRepository;
import com.SouthMillion.chat_service.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private MutedPlayerRepository mutedRepository;

    @InjectMocks
    private ChatService chatService;

    private static final String SENDER_ID = "1001";
    private static final String SENDER_NAME = "Player1";

    // =========================================================
    // sendMessage
    // =========================================================
    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("TC-CHAT-001 [P] Gui tin nhan kenh World thanh cong")
        void sendMessage_worldChannel_success() {
            ChatDTO.SendMessageRequest req = ChatDTO.SendMessageRequest.builder()
                    .channel(1)
                    .senderId(SENDER_ID)
                    .senderName(SENDER_NAME)
                    .content("Hello World!")
                    .build();

            given(mutedRepository.existsByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(false);

            ChatMessage saved = new ChatMessage();
            saved.setId(1L);
            saved.setChannel(1);
            saved.setSenderId(SENDER_ID);
            saved.setSenderName(SENDER_NAME);
            saved.setContent("Hello World!");
            given(messageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatDTO.Response<ChatDTO.MessageInfo> resp = chatService.sendMessage(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData().getSenderId()).isEqualTo(SENDER_ID);
        }

        @Test
        @DisplayName("TC-CHAT-002 [N] Nguoi dung bi mute – tra ve error(-1)")
        void sendMessage_mutedPlayer_returnsError() {
            ChatDTO.SendMessageRequest req = ChatDTO.SendMessageRequest.builder()
                    .channel(1)
                    .senderId(SENDER_ID)
                    .senderName(SENDER_NAME)
                    .content("Should not send")
                    .build();

            given(mutedRepository.existsByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(true);

            ChatDTO.Response<ChatDTO.MessageInfo> resp = chatService.sendMessage(req);

            assertThat(resp.getCode()).isEqualTo(-1);
            assertThat(resp.getMessage()).containsIgnoringCase("muted");
            then(messageRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-CHAT-003 [N] Private chat khong co receiverId – tra ve error(-2)")
        void sendMessage_privateChat_noReceiver_returnsError() {
            ChatDTO.SendMessageRequest req = ChatDTO.SendMessageRequest.builder()
                    .channel(4)
                    .senderId(SENDER_ID)
                    .senderName(SENDER_NAME)
                    .receiverId(null)
                    .content("Private message")
                    .build();

            given(mutedRepository.existsByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(false);

            ChatDTO.Response<ChatDTO.MessageInfo> resp = chatService.sendMessage(req);

            assertThat(resp.getCode()).isEqualTo(-2);
            then(messageRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-CHAT-004 [P] Private chat co receiverId – gui thanh cong")
        void sendMessage_privateChat_withReceiver_success() {
            ChatDTO.SendMessageRequest req = ChatDTO.SendMessageRequest.builder()
                    .channel(4)
                    .senderId(SENDER_ID)
                    .senderName(SENDER_NAME)
                    .receiverId("1002")
                    .receiverName("Player2")
                    .content("Hello!")
                    .build();

            given(mutedRepository.existsByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(false);

            ChatMessage saved = new ChatMessage();
            saved.setId(2L);
            saved.setChannel(4);
            saved.setSenderId(SENDER_ID);
            saved.setReceiverId("1002");
            saved.setContent("Hello!");
            given(messageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatDTO.Response<ChatDTO.MessageInfo> resp = chatService.sendMessage(req);

            assertThat(resp.getCode()).isZero();
        }

        @Test
        @DisplayName("TC-CHAT-005 [P] Gui tin Guild chat – thanh cong")
        void sendMessage_guildChannel_success() {
            ChatDTO.SendMessageRequest req = ChatDTO.SendMessageRequest.builder()
                    .channel(2)
                    .senderId(SENDER_ID)
                    .senderName(SENDER_NAME)
                    .channelId("guild-1")
                    .content("Guild message")
                    .build();

            given(mutedRepository.existsByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(false);

            ChatMessage saved = new ChatMessage();
            saved.setId(3L);
            saved.setChannel(2);
            given(messageRepository.save(any(ChatMessage.class))).willReturn(saved);

            ChatDTO.Response<ChatDTO.MessageInfo> resp = chatService.sendMessage(req);

            assertThat(resp.getCode()).isZero();
        }
    }

    // =========================================================
    // getHistory
    // =========================================================
    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("TC-CHAT-010 [P] Lay lich su World chat")
        void getHistory_worldChannel_returnMessages() {
            ChatMessage msg = new ChatMessage();
            msg.setId(1L);
            msg.setChannel(1);
            msg.setSenderId(SENDER_ID);
            msg.setContent("Hi");

            given(messageRepository.findByChannelOrderByCreatedAtDesc(eq(1), any(PageRequest.class)))
                    .willReturn(List.of(msg));

            ChatDTO.GetHistoryRequest req = ChatDTO.GetHistoryRequest.builder()
                    .channel(1)
                    .count(50)
                    .build();

            ChatDTO.Response<List<ChatDTO.MessageInfo>> resp = chatService.getHistory(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).hasSize(1);
        }

        @Test
        @DisplayName("TC-CHAT-011 [P] Lay lich su Private chat")
        void getHistory_privateChannel_returnsMessages() {
            ChatMessage msg = new ChatMessage();
            msg.setId(2L);
            msg.setChannel(4);
            msg.setSenderId(SENDER_ID);
            msg.setReceiverId("1002");
            msg.setContent("Private msg");

            given(messageRepository.findPrivateChat(eq("1001"), eq("1002"), any(PageRequest.class)))
                    .willReturn(List.of(msg));

            ChatDTO.GetHistoryRequest req = ChatDTO.GetHistoryRequest.builder()
                    .channel(4)
                    .roleId1("1001")
                    .roleId2("1002")
                    .count(50)
                    .build();

            ChatDTO.Response<List<ChatDTO.MessageInfo>> resp = chatService.getHistory(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).hasSize(1);
        }

        @Test
        @DisplayName("TC-CHAT-012 [P] Lay lich su Guild chat")
        void getHistory_guildChannel_returnsMessages() {
            given(messageRepository.findByChannelAndChannelIdOrderByCreatedAtDesc(
                    eq(2), eq("guild-1"), any(PageRequest.class)))
                    .willReturn(List.of());

            ChatDTO.GetHistoryRequest req = ChatDTO.GetHistoryRequest.builder()
                    .channel(2)
                    .channelId("guild-1")
                    .count(50)
                    .build();

            ChatDTO.Response<List<ChatDTO.MessageInfo>> resp = chatService.getHistory(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).isEmpty();
        }
    }

    // =========================================================
    // mutePlayer / unmutePlayer
    // =========================================================
    @Nested
    @DisplayName("mutePlayer() / unmutePlayer()")
    class MuteUnmute {

        @Test
        @DisplayName("TC-CHAT-020 [P] Mute nguoi choi 60 phut – thanh cong")
        void mutePlayer_success() {
            ChatDTO.MuteRequest req = ChatDTO.MuteRequest.builder()
                    .roleId(SENDER_ID)
                    .roleName(SENDER_NAME)
                    .durationMinutes(60)
                    .reason("Spam")
                    .build();

            given(mutedRepository.save(any(MutedPlayer.class))).willAnswer(inv -> inv.getArgument(0));

            ChatDTO.Response<Void> resp = chatService.mutePlayer(req);

            assertThat(resp.getCode()).isZero();
            then(mutedRepository).should().save(any(MutedPlayer.class));
        }

        @Test
        @DisplayName("TC-CHAT-021 [P] Unmute nguoi choi – thanh cong")
        void unmutePlayer_success() {
            MutedPlayer muted = new MutedPlayer();
            given(mutedRepository.findByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(Optional.of(muted));

            ChatDTO.Response<Void> resp = chatService.unmutePlayer(SENDER_ID);

            assertThat(resp.getCode()).isZero();
            then(mutedRepository).should().delete(muted);
        }

        @Test
        @DisplayName("TC-CHAT-022 [P] Unmute nguoi khong bi mute – khong lam gi")
        void unmutePlayer_notMuted_noOp() {
            given(mutedRepository.findByRoleIdAndMuteUntilAfter(anyLong(), any(LocalDateTime.class)))
                    .willReturn(Optional.empty());

            ChatDTO.Response<Void> resp = chatService.unmutePlayer(SENDER_ID);

            assertThat(resp.getCode()).isZero();
            then(mutedRepository).should(never()).delete(any());
        }
    }
}
