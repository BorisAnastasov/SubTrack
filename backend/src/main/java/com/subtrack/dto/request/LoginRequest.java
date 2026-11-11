package com.subtrack.dto.request;

import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = MessageConstants.VALIDATION_EMAIL_REQUIRED)
        @Email(message = MessageConstants.VALIDATION_EMAIL_INVALID)
        String email,

        @NotBlank(message = MessageConstants.VALIDATION_PASSWORD_REQUIRED)
        String password
) {  }