package com.subtrack.dto.request;

import com.subtrack.model.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionStatusRequest(@NotNull SubscriptionStatus status) {
}
