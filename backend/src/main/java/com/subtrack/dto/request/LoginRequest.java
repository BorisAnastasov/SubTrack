package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = MessageConstants.VALIDATION_EMAIL_REQUIRED)
        @Email(message = MessageConstants.VALIDATION_EMAIL_INVALID)
        String email,

        @NotBlank(message = MessageConstants.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = DbConstants.PASSWORD_MIN_LENGTH, max = DbConstants.PASSWORD_MAX_LENGTH,
                message = MessageConstants.VALIDATION_PASSWORD_LENGTH)
        String password
) {  }