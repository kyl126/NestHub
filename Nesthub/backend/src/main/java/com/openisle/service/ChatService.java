package com.openisle.service;

import com.openisle.model.*;
import com.openisle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;

        @Transactional
    public ChatRoom getOrCreateChannelRoom(Long channelId, String channelName) {
                return chatRoomRepository.findFirstByChannelIdAndType(channelId, "CHANNEL_DEFAULT")
            .orElseGet(() -> {
                ChatRoom room = new ChatRoom();
                room.setChannelId(channelId);
                room.setName(channelId == 0 ? "全站聊天" : channelName + "-聊天");
                room.setType("CHANNEL_DEFAULT");
                return chatRoomRepository.save(room);
            });
    }

    @Transactional
    public ChatRoom getOrCreatePopupRoom(Long channelId, String channelName) {
        return chatRoomRepository.findByChannelIdAndType(channelId, "CHANNEL_POPUP")
            .orElseGet(() -> {
                ChatRoom room = new ChatRoom();
                room.setChannelId(channelId);
                room.setName(channelId == 0 ? "全站弹窗" : channelName + "-弹窗");
                room.setType("CHANNEL_POPUP");
                return chatRoomRepository.save(room);
            });
    }

    @Transactional
    public ChatMessage sendMessage(Long roomId, Long senderId, String content) {
        User sender = userRepository.findById(senderId).orElseThrow();
        if (sender.isBanned()) {
            throw new IllegalArgumentException("您已被封禁，无法发送消息");
        }
        if (sender.getMutedUntil() != null && sender.getMutedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("您已被禁言，无法发送消息");
        }

        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        ChatMessage saved = chatMessageRepository.save(msg);
        saved.setSender(sender);

        if (!chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, senderId)) {
            ChatRoomMember member = new ChatRoomMember();
            member.setRoomId(roomId);
            member.setUserId(senderId);
            chatRoomMemberRepository.save(member);
        }

        // 推送到聊天房间
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, saved);

        // 推送弹窗通知到公共 topic，前端自己过滤
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        String roomName = room != null ? room.getName() : "聊天";

        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", sender.getId());
        senderMap.put("username", sender.getUsername());
        senderMap.put("avatar", sender.getAvatar());

        Map<String, Object> notificationPayload = new HashMap<>();
        notificationPayload.put("sender", senderMap);
        notificationPayload.put("content", content);
        notificationPayload.put("roomId", roomId);
        notificationPayload.put("channelId", room != null ? room.getChannelId() : 0L);
        notificationPayload.put("roomName", roomName);

        messagingTemplate.convertAndSend("/topic/chat-notification", notificationPayload);


        System.out.println("[ChatService] 通过 RabbitMQ 发送弹窗通知: " + notificationPayload);
        
        try {
            String routingKey = "notifications.shard." + Integer.toHexString(Math.abs(roomId.hashCode()) % 16);
            
            // 包装成 MessageNotificationPayload 兼容格式
            Map<String, Object> rabbitPayload = new HashMap<>();
            rabbitPayload.put("targetUsername", "chat-notification");
            rabbitPayload.put("payload", notificationPayload);
            
            rabbitTemplate.convertAndSend("openisle-exchange", routingKey, rabbitPayload);
            System.out.println("[ChatService] RabbitMQ 弹窗通知已发送, routingKey=" + routingKey);
        } catch (Exception e) {
            System.err.println("[ChatService] RabbitMQ 弹窗通知发送失败: " + e.getMessage());
        }

        return saved;
    }

    public List<ChatMessage> getRecentMessages(Long roomId, int limit) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit));
    }

    public List<ChatMessage> getMessagesSince(Long roomId, Long sinceId) {
        return chatMessageRepository.findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(roomId, sinceId);
    }
}