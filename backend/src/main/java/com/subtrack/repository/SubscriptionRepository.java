package com.subtrack.repository;

import com.subtrack.model.Subscription;
import com.subtrack.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate date);

    @EntityGraph(attributePaths = "user")
    List<Subscription> findByStatusAndNextBillingDateBetween(
            SubscriptionStatus status, LocalDate fromDate, LocalDate toDate);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);
}