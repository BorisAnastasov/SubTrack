package com.subtrack.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(@NotBlank String token) {
}
