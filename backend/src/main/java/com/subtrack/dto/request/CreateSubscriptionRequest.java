package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import com.subtrack.constant.MessageConstants;
import com.subtrack.constant.ValidationConstants;
import com.subtrack.model.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        Long categoryId,

        @NotBlank(message = MessageConstants.VALIDATION_NAME_REQUIRED)
        @Size(max = DbConstants.SUBSCRIPTION_NAME_MAX_LENGTH)
        String name,

        @NotNull(message = MessageConstants.VALIDATION_PRICE_REQUIRED)
        @DecimalMin(value = ValidationConstants.MIN_SUBSCRIPTION_PRICE, inclusive = true,
                message = MessageConstants.VALIDATION_PRICE_POSITIVE)
        @Digits(integer = DbConstants.DECIMAL_PRECISION - DbConstants.DECIMAL_SCALE,
                fraction = DbConstants.DECIMAL_SCALE)
        BigDecimal price,

        @NotBlank(message = MessageConstants.VALIDATION_CURRENCY_REQUIRED)
        @Size(min = DbConstants.CURRENCY_CODE_LENGTH, max = DbConstants.CURRENCY_CODE_LENGTH)
        @Pattern(regexp = ValidationConstants.CURRENCY_REGEX,
                message = MessageConstants.VALIDATION_CURRENCY_FORMAT)
        String currency,

        @NotNull(message = MessageConstants.VALIDATION_BILLING_CYCLE_REQUIRED)
        BillingCycle billingCycle,

        @NotNull(message = MessageConstants.VALIDATION_START_DATE_REQUIRED)
        LocalDate nextBillingDate,

        @Size(max = DbConstants.PAYMENT_METHOD_MAX_LENGTH)
        String paymentMethod,

        String notes
) { }
