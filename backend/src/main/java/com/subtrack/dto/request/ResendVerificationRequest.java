package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(
        @NotBlank(message = MessageConstants.VALIDATION_EMAIL_REQUIRED)
        @Email(message = MessageConstants.VALIDATION_EMAIL_INVALID)
        @Size(max = DbConstants.EMAIL_MAX_LENGTH)
        String email
) {
}
