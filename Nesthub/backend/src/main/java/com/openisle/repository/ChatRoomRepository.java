package com.openisle.repository;

import com.openisle.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByChannelId(Long channelId);
    Optional<ChatRoom> findByChannelIdAndType(Long channelId, String type);
    Optional<ChatRoom> findFirstByChannelIdAndType(Long channelId, String type);
}