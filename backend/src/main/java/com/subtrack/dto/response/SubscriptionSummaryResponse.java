package com.subtrack.dto.response;

import java.util.List;

public record SubscriptionSummaryResponse(
        String displayCurrency,
        List<CurrencyTotalResponse> monthlyCost,
        List<CurrencyTotalResponse> annualCost,
        List<CategorySpendingResponse> spendingByCategory
) {
}
