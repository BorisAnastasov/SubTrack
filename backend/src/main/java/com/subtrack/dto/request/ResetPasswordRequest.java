package com.subtrack.dto.request;

import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = MessageConstants.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = 6, max = 100, message = MessageConstants.VALIDATION_PASSWORD_LENGTH)
        String password
) { }
