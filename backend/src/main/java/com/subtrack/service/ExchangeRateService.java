package com.subtrack.service;

import com.subtrack.dto.response.CurrencyConversionResponse;
import com.subtrack.dto.response.ExchangeRatesResponse;

import java.math.BigDecimal;

public interface ExchangeRateService {
    ExchangeRatesResponse getRates(String baseCurrency);

    CurrencyConversionResponse convert(BigDecimal amount, String fromCurrency, String toCurrency);
}
