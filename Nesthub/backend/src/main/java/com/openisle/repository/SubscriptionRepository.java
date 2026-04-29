package com.openisle.repository;

import com.openisle.model.User;
import com.openisle.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    void deleteBySubscriberAndTarget(User subscriber, User target);
    
    boolean existsBySubscriberAndTarget(User subscriber, User target);
}