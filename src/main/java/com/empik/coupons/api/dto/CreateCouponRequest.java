package com.empik.coupons.api.dto;

import com.empik.coupons.api.validation.ValidCountryCode;
import jakarta.validation.constraints.*;

public record CreateCouponRequest(

        @NotBlank(message = "Coupon code must not be blank")
        @Size(min = 3, max = 100, message = "Coupon code must be between 3 and 100 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Coupon code can only contain letters, digits, hyphens and underscores")
        String code,

        @NotBlank(message = "Country code must not be blank")
        @ValidCountryCode
        String countryCode,

        @Min(value = 1, message = "Max usages must be at least 1")
        @Max(value = 1_000_000, message = "Max usages must not exceed 1,000,000")
        int maxUsages
) {}
