package com.subtrack.dto.response;

import java.math.BigDecimal;

public record CurrencyTotalResponse(String currency, BigDecimal amount) {
}
