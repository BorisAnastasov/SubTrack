package com.subtrack.service;

import com.subtrack.dto.request.CreateSubscriptionRequest;
import com.subtrack.dto.request.UpdateSubscriptionRequest;
import com.subtrack.dto.response.SubscriptionSummaryResponse;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.dto.response.SubscriptionResponse;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionService {
    List<SubscriptionResponse> getSubscriptions(Long userId);

    List<SubscriptionResponse> searchSubscriptions(
            Long userId,
            String query,
            SubscriptionStatus status,
            Long categoryId,
            BillingCycle billingCycle,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDirection);

    List<SubscriptionResponse> getUpcomingSubscriptions(Long userId, int days);

    SubscriptionSummaryResponse getSummary(Long userId);

    SubscriptionSummaryResponse getSummary(Long userId, String displayCurrency);

    String exportSubscriptions(Long userId);

    SubscriptionResponse createSubscription(Long userId, @Valid CreateSubscriptionRequest subscriptionRequest);

    SubscriptionResponse editSubscription(
            Long userId, Long subscriptionId, @Valid UpdateSubscriptionRequest subscriptionRequest);

    SubscriptionResponse updateStatus(Long userId, Long subscriptionId, SubscriptionStatus status);
}
