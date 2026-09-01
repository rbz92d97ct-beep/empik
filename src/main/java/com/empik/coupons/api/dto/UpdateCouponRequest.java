package com.empik.coupons.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCouponRequest(
        @Min(value = 1, message = "Max usages must be at least 1")
        @Max(value = 1_000_000, message = "Max usages must not exceed 1,000,000")
        int maxUsages
) {}
