package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = MessageConstants.VALIDATION_EMAIL_REQUIRED)
        @Email(message = MessageConstants.VALIDATION_EMAIL_INVALID)
        @Size(max = DbConstants.EMAIL_MAX_LENGTH)
        String email,

        @NotBlank(message = MessageConstants.VALIDATION_PASSWORD_REQUIRED)
        @Size(min = 6, max = 100, message = MessageConstants.VALIDATION_PASSWORD_LENGTH)
        String password
) { }