package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = MessageConstants.VALIDATION_RESET_TOKEN_REQUIRED)
        String token,

        @NotBlank(message = MessageConstants.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = DbConstants.PASSWORD_MIN_LENGTH, max = DbConstants.PASSWORD_MAX_LENGTH,
                message = MessageConstants.VALIDATION_PASSWORD_LENGTH)
        String password
) { }
