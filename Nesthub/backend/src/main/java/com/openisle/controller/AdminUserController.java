package com.openisle.controller;

import com.openisle.model.Notification;
import com.openisle.model.NotificationType;
import com.openisle.model.User;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.UserRepository;
import com.openisle.service.EmailSender;
import com.openisle.exception.EmailSendException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final EmailSender emailSender;

  @Value("${app.website-url}")
  private String websiteUrl;

  @PostMapping("/{id}/approve")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Approve user", description = "Approve a pending user registration")
  @ApiResponse(responseCode = "200", description = "User approved")
  public ResponseEntity<?> approve(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setApproved(true);
    userRepository.save(user);
    markRegisterRequestNotificationsRead(user);
    try {
      emailSender.sendEmail(
        user.getEmail(),
        "您的注册已审核通过",
        "🎉您的注册已经审核通过, 点击以访问网站: " + websiteUrl
      );
    } catch (EmailSendException e) {
      log.warn("Failed to send approve email to {}: {}", user.getEmail(), e.getMessage());
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/reject")
  @SecurityRequirement(name = "JWT")
  @Operation(summary = "Reject user", description = "Reject a pending user registration")
  @ApiResponse(responseCode = "200", description = "User rejected")
  public ResponseEntity<?> reject(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setApproved(false);
    userRepository.save(user);
    markRegisterRequestNotificationsRead(user);
    try {
      emailSender.sendEmail(
        user.getEmail(),
        "您的注册已被管理员拒绝",
        "您的注册被管理员拒绝, 点击链接可以重新填写理由申请: " + websiteUrl
      );
    } catch (EmailSendException e) {
      log.warn("Failed to send reject email to {}: {}", user.getEmail(), e.getMessage());
    }
    return ResponseEntity.ok().build();
  }

  private void markRegisterRequestNotificationsRead(User applicant) {
    java.util.List<Notification> notifs = notificationRepository.findByTypeAndFromUser(
        NotificationType.REGISTER_REQUEST,
        applicant);
    for (Notification n : notifs) {
      n.setRead(true);
    }
    notificationRepository.saveAll(notifs);
  }

  // ========== 用户列表 ==========
  @GetMapping
  @SecurityRequirement(name = "JWT")
  public java.util.List<User> listUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return userRepository.findAllByOrderByIdAsc(
        org.springframework.data.domain.PageRequest.of(page, size));
  }

  // ========== 封禁/解封 ==========
  @PostMapping("/{id}/ban")
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<?> ban(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setBanned(true);
    userRepository.save(user);
    return ResponseEntity.ok(java.util.Map.of("message", "用户已封禁"));
  }

  @PostMapping("/{id}/unban")
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<?> unban(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setBanned(false);
    userRepository.save(user);
    return ResponseEntity.ok(java.util.Map.of("message", "用户已解封"));
  }

  // ========== 禁言 ==========
  @PostMapping("/{id}/mute")
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<?> mute(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> payload) {
    int days = payload.getOrDefault("days", 7);
    User user = userRepository.findById(id).orElseThrow();
    user.setMutedUntil(java.time.LocalDateTime.now().plusDays(days));
    userRepository.save(user);
    return ResponseEntity.ok(java.util.Map.of("message", "用户已禁言 " + days + " 天"));
  }

  @PostMapping("/{id}/unmute")
  @SecurityRequirement(name = "JWT")
  public ResponseEntity<?> unmute(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setMutedUntil(null);
    userRepository.save(user);
    return ResponseEntity.ok(java.util.Map.of("message", "已解除禁言"));
  }
}