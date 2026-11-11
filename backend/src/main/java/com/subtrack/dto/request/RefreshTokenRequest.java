package com.subtrack.dto.request;

import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = MessageConstants.VALIDATION_REFRESH_TOKEN_REQUIRED)
        String refreshToken
) {
}