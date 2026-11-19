package com.subtrack.dto.response;

import java.math.BigDecimal;

public record CategorySpendingResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        String currency,
        BigDecimal monthlyCost,
        BigDecimal annualCost
) { }
