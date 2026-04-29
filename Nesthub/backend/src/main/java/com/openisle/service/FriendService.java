package com.openisle.service;

import com.openisle.model.*;
import com.openisle.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Data
@RequiredArgsConstructor
public class FriendService {
    
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final SubscriptionRepository subscriptionRepository;
    // 发送好友申请
    public FriendRequest sendRequest(String senderName, String receiverName, String message) {
        User sender = userRepository.findByUsername(senderName)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        User receiver = userRepository.findByUsername(receiverName)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        
        if (friendRequestRepository.existsBySenderAndReceiver(sender, receiver)) {
            throw new IllegalArgumentException("已发送过好友申请");
        }
        
        if (friendshipRepository.existsByUserAndFriend(sender, receiver)) {
            throw new IllegalArgumentException("已经是好友");
        }
        
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setMessage(message);
        request.setStatus(FriendRequest.FriendRequestStatus.PENDING);
        request.setCreatedAt(java.time.LocalDateTime.now());
        request.setUpdatedAt(java.time.LocalDateTime.now());
        
        friendRequestRepository.save(request);
        
        // 发送通知
        notificationService.createNotification(
            receiver, 
            NotificationType.FRIEND_REQUEST, 
            null, null, null, 
            sender, null, 
            message != null ? message : "请求添加你为好友"
        );
        
        return request;
    }

    // 接受好友申请
    @Transactional
    public void acceptRequest(Long requestId, String username) {
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        User receiver = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("无权操作");
        }
        
        if (request.getStatus() != FriendRequest.FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("申请已处理");
        }
        
        request.setStatus(FriendRequest.FriendRequestStatus.ACCEPTED);
        request.setUpdatedAt(java.time.LocalDateTime.now());
        friendRequestRepository.save(request);
        
        // 建立双向好友关系
        Friendship f1 = new Friendship();
        f1.setUser(request.getSender());
        f1.setFriend(request.getReceiver());
        f1.setCreatedAt(java.time.LocalDateTime.now());
        
        Friendship f2 = new Friendship();
        f2.setUser(request.getReceiver());
        f2.setFriend(request.getSender());
        f2.setCreatedAt(java.time.LocalDateTime.now());
        
        friendshipRepository.save(f1);
        friendshipRepository.save(f2);
    }

    // 拒绝好友申请
    public void rejectRequest(Long requestId, String username) {
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        User receiver = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("无权操作");
        }
        
        if (request.getStatus() != FriendRequest.FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("申请已处理");
        }
        
        request.setStatus(FriendRequest.FriendRequestStatus.REJECTED);
        request.setUpdatedAt(java.time.LocalDateTime.now());
        friendRequestRepository.save(request);
    }

    // 删除好友
    @Transactional
    public void deleteFriend(String username, String friendName) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        User friend = userRepository.findByUsername(friendName)
            .orElseThrow(() -> new IllegalArgumentException("好友不存在"));
        
        // 1. 删除双向好友关系
        friendshipRepository.deleteByUserAndFriend(user, friend);
        friendshipRepository.deleteByUserAndFriend(friend, user);
        
        // 2. 删除两人之间的所有好友申请记录
        friendRequestRepository.deleteBySenderAndReceiver(user, friend);
        friendRequestRepository.deleteBySenderAndReceiver(friend, user);

        // 3. 双向取消关注
        subscriptionRepository.deleteBySubscriberAndTarget(user, friend);
        subscriptionRepository.deleteBySubscriberAndTarget(friend, user);
    }

    // 获取好友列表
    public List<User> getFriends(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        return friendshipRepository.findByUser(user)
            .stream()
            .map(Friendship::getFriend)
            .toList();
    }

    // 获取收到的好友申请
    public List<FriendRequest> getPendingRequests(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.FriendRequestStatus.PENDING);
    }
    
    // 检查是否是好友
    public boolean isFriend(String username, String targetName) {
        User user = userRepository.findByUsername(username).orElse(null);
        User target = userRepository.findByUsername(targetName).orElse(null);
        if (user == null || target == null) return false;
        return friendshipRepository.existsByUserAndFriend(user, target);
    }
}