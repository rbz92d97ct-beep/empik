package com.empik.coupons.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemCouponRequest(

        @NotBlank(message = "User ID must not be blank")
        @Size(max = 255, message = "User ID must not exceed 255 characters")
        String userId
) {}
