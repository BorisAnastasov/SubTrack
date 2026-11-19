package com.subtrack.dto.response;

import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String categoryColor,
        String name,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        LocalDate nextBillingDate,
        String paymentMethod,
        SubscriptionStatus status,
        String notes,
        List<SubscriptionStatusChangeResponse> statusHistory
) {
}
