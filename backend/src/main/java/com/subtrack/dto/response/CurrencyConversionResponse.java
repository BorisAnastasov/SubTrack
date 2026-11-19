package com.subtrack.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyConversionResponse(
        BigDecimal amount,
        String fromCurrency,
        BigDecimal convertedAmount,
        String toCurrency,
        BigDecimal exchangeRate,
        LocalDate rateDate
) {
}
