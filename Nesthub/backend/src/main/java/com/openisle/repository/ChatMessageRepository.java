package com.openisle.repository;

import com.openisle.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(Long roomId, Long sinceId);
}