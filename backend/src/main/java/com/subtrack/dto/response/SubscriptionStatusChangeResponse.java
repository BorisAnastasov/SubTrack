package com.subtrack.dto.response;

import com.subtrack.model.enums.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionStatusChangeResponse(
        SubscriptionStatus fromStatus,
        SubscriptionStatus toStatus,
        LocalDateTime changedAt
) {
}
