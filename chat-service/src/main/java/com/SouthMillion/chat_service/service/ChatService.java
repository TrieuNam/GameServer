package com.SouthMillion.chat_service.service;

import com.SouthMillion.chat_service.dto.ChatDTO;
import com.SouthMillion.chat_service.entity.*;
import com.SouthMillion.chat_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final MutedPlayerRepository mutedRepository;

    @Transactional
    public ChatDTO.Response<ChatDTO.MessageInfo> sendMessage(ChatDTO.SendMessageRequest request) {
        log.info("Sending message: channel={}, sender={}", request.getChannel(), request.getSenderId());

        // Check if muted
        if (mutedRepository.existsByRoleIdAndMuteUntilAfter(Long.parseLong(request.getSenderId()), LocalDateTime.now())) {
            return ChatDTO.Response.error(-1, "You are muted");
        }

        // Validate private chat
        if (request.getChannel() == 4 && (request.getReceiverId() == null || request.getReceiverId().isEmpty())) {
            return ChatDTO.Response.error(-2, "Private chat requires receiver ID");
        }

        ChatMessage message = ChatMessage.builder()
                .channel(request.getChannel())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .receiverId(request.getReceiverId())
                .receiverName(request.getReceiverName())
                .channelId(request.getChannelId())
                .content(request.getContent())
                .build();

        message = messageRepository.save(message);
        log.info("Message sent: id={}", message.getId());

        return ChatDTO.Response.success(buildMessageInfo(message));
    }

    @Transactional(readOnly = true)
    public ChatDTO.Response<List<ChatDTO.MessageInfo>> getHistory(ChatDTO.GetHistoryRequest request) {
        log.info("Getting chat history: channel={}", request.getChannel());

        List<ChatMessage> messages;
        PageRequest pageable = PageRequest.of(0, request.getCount());

        if (request.getChannel() == 4) {
            // Private chat
            messages = messageRepository.findPrivateChat(request.getRoleId1(), request.getRoleId2(), pageable);
        } else if (request.getChannel() == 2 || request.getChannel() == 3) {
            // Guild or Team chat
            messages = messageRepository.findByChannelAndChannelIdOrderByCreatedAtDesc(
                    request.getChannel(), request.getChannelId(), pageable);
        } else {
            // World or System chat
            messages = messageRepository.findByChannelOrderByCreatedAtDesc(request.getChannel(), pageable);
        }

        List<ChatDTO.MessageInfo> messageInfos = messages.stream()
                .map(this::buildMessageInfo)
                .collect(Collectors.toList());

        return ChatDTO.Response.success(messageInfos);
    }

    @Transactional
    public ChatDTO.Response<Void> mutePlayer(ChatDTO.MuteRequest request) {
        log.info("Muting player: roleId={}, duration={} minutes", request.getRoleId(), request.getDurationMinutes());

        LocalDateTime muteUntil = LocalDateTime.now().plusMinutes(request.getDurationMinutes());

        MutedPlayer muted = MutedPlayer.builder()
                .roleId(Long.parseLong(request.getRoleId()))
                .roleName(request.getRoleName())
                .reason(request.getReason())
                .muteUntil(muteUntil)
                .build();

        mutedRepository.save(muted);
        log.info("Player muted until: {}", muteUntil);

        return ChatDTO.Response.success(null);
    }

    @Transactional
    public ChatDTO.Response<Void> unmutePlayer(String roleId) {
        log.info("Unmuting player: roleId={}", roleId);

        mutedRepository.findByRoleIdAndMuteUntilAfter(Long.parseLong(roleId), LocalDateTime.now())
                .ifPresent(mutedRepository::delete);

        return ChatDTO.Response.success(null);
    }

    private ChatDTO.MessageInfo buildMessageInfo(ChatMessage message) {
        return ChatDTO.MessageInfo.builder()
                .id(message.getId())
                .channel(message.getChannel())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
