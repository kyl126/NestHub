package com.openisle.repository;

import com.openisle.model.FriendRequest;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    
    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequest.FriendRequestStatus status);
    
    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.FriendRequestStatus status);
    
    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, FriendRequest.FriendRequestStatus status);
    
    boolean existsBySenderAndReceiver(User sender, User receiver);
    
    // 👇 添加这个方法
    void deleteBySenderAndReceiver(User sender, User receiver);
}