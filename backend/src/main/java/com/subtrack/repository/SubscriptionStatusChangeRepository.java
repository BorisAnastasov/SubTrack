package com.subtrack.repository;

import com.subtrack.model.SubscriptionStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionStatusChangeRepository extends JpaRepository<SubscriptionStatusChange, Long> {
    List<SubscriptionStatusChange> findBySubscriptionIdOrderByChangedAtDesc(Long subscriptionId);
}
