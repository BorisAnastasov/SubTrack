package com.subtrack.repository;

import com.subtrack.model.SubscriptionReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SubscriptionReminderRepository extends JpaRepository<SubscriptionReminder, Long> {
    boolean existsBySubscriptionIdAndBillingDate(Long subscriptionId, LocalDate billingDate);
}
