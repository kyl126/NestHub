package com.openisle.controller;

import com.openisle.model.FriendRequest;
import com.openisle.model.User;
import com.openisle.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    
    // 发送好友申请
    @PostMapping("/request/{username}")
    public ResponseEntity<?> sendRequest(
        @PathVariable String username,
        @RequestBody Map<String, String> body,
        Authentication auth
    ) {
        try {
            String message = body.getOrDefault("message", "");
            friendService.sendRequest(auth.getName(), username, message);
            return ResponseEntity.ok(Map.of("message", "好友申请已发送"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 获取好友列表
    @GetMapping("/list")
    public ResponseEntity<List<User>> getFriends(Authentication auth) {
        return ResponseEntity.ok(friendService.getFriends(auth.getName()));
    }
    
    // 获取待处理的好友申请
    @GetMapping("/requests/pending")
    public ResponseEntity<List<FriendRequest>> getPendingRequests(Authentication auth) {
        return ResponseEntity.ok(friendService.getPendingRequests(auth.getName()));
    }
    
    // 接受好友申请
    @PostMapping("/request/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, Authentication auth) {
        try {
            friendService.acceptRequest(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "已接受好友申请"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 拒绝好友申请
    @PostMapping("/request/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, Authentication auth) {
        try {
            friendService.rejectRequest(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "已拒绝好友申请"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 删除好友
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteFriend(@PathVariable String username, Authentication auth) {
        try {
            friendService.deleteFriend(auth.getName(), username);
            return ResponseEntity.ok(Map.of("message", "已删除好友"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
