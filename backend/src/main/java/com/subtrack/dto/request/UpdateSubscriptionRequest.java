package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import com.subtrack.constant.ValidationConstants;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSubscriptionRequest(
        Long categoryId,

        @NotBlank(message = MessageConstants.VALIDATION_NAME_REQUIRED)
        @Size(max = DbConstants.SUBSCRIPTION_NAME_MAX_LENGTH,
                message = MessageConstants.VALIDATION_NAME_TOO_LONG)
        String name,

        @DecimalMin(value = ValidationConstants.MIN_SUBSCRIPTION_PRICE, inclusive = true,
                message = MessageConstants.VALIDATION_PRICE_POSITIVE)
        @Digits(integer = DbConstants.DECIMAL_PRECISION - DbConstants.DECIMAL_SCALE,
                fraction = DbConstants.DECIMAL_SCALE)
        BigDecimal price,

        @Size(min = DbConstants.CURRENCY_CODE_LENGTH, max = DbConstants.CURRENCY_CODE_LENGTH)
        @Pattern(regexp = ValidationConstants.CURRENCY_REGEX,
                message = MessageConstants.VALIDATION_CURRENCY_FORMAT)
        String currency,

        BillingCycle billingCycle,

        LocalDate nextBillingDate,

        @Size(max = DbConstants.PAYMENT_METHOD_MAX_LENGTH)
        String paymentMethod,

        SubscriptionStatus status,

        String notes
) { }
