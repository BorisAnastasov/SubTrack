package com.subtrack.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewalReminderEmailItem(
        String subscriptionName,
        BigDecimal price,
        String currency,
        LocalDate billingDate
) {
}
