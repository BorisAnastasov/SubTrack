package com.subtrack.repository;

import com.subtrack.model.Subscription;
import com.subtrack.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    // За планираната задача (ReminderScheduler) за предстоящи падежи:
    List<Subscription> findByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate date);
}