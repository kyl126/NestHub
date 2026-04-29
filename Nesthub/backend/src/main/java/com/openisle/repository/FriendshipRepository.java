package com.openisle.repository;

import com.openisle.model.Friendship;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUser(User user);
    void deleteByUserAndFriend(User user, User friend);
    boolean existsByUserAndFriend(User user, User friend);
}