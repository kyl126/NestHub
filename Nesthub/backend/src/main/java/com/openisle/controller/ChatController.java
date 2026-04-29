package com.openisle.controller;

import com.openisle.model.Category;
import com.openisle.model.ChatMessage;
import com.openisle.model.ChatRoom;
import com.openisle.service.ChatService;
import com.openisle.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CategoryService categoryService;
    private final com.openisle.repository.UserRepository userRepository;

    @GetMapping("/rooms/{channelId}")
    public ChatRoom getChannelRoom(@PathVariable Long channelId) {
        Category category = categoryService.getCategory(channelId);
        return chatService.getOrCreateChannelRoom(channelId, category.getName());
    }

    @GetMapping("/popup-room/{channelId}")
    public ChatRoom getPopupRoom(@PathVariable Long channelId) {
        Category category = categoryService.getCategory(channelId);
        return chatService.getOrCreateChannelRoom(channelId, category.getName());
    }

    @GetMapping("/rooms/{roomId}/messages")
    @SecurityRequirement(name = "JWT")
    public List<ChatMessage> getMessages(@PathVariable Long roomId,
        @RequestParam(defaultValue = "50") int limit) {
        return chatService.getRecentMessages(roomId, limit);
        }

    @GetMapping("/rooms/{roomId}/messages/since")
    @SecurityRequirement(name = "JWT")
    public List<ChatMessage> getMessagesSince(@PathVariable Long roomId,
                                              @RequestParam Long sinceId) {
        return chatService.getMessagesSince(roomId, sinceId);
    }

    @PostMapping("/rooms/{roomId}/messages")
    @SecurityRequirement(name = "JWT")
    public ChatMessage sendMessage(@PathVariable Long roomId,
                                   @RequestBody SendMessageRequest req,
                                   Authentication auth) {
        Long userId = userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"))
            .getId();
        return chatService.sendMessage(roomId, userId, req.getContent());
    }

    @lombok.Data
    static class SendMessageRequest {
        private String content;
    }
}