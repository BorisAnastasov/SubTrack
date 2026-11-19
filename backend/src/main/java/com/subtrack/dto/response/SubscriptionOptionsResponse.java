package com.subtrack.dto.response;

import java.util.List;

public record SubscriptionOptionsResponse(
        List<EnumOptionResponse> billingCycles,
        List<EnumOptionResponse> statuses,
        List<CategoryOptionResponse> categories
) { }
