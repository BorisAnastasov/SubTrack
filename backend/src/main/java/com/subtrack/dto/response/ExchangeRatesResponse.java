package com.subtrack.dto.response;

import java.time.LocalDate;
import java.util.Map;

public record ExchangeRatesResponse(String baseCurrency, LocalDate rateDate, Map<String, Double> rates) {
}
