package com.empik.coupons.api.dto;

import com.empik.coupons.domain.Coupon;

import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        String countryCode,
        int maxUsages,
        int currentUsages,
        int remainingUsages,
        Instant createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getCountryCode(),
                coupon.getMaxUsages(),
                coupon.getCurrentUsages(),
                coupon.getMaxUsages() - coupon.getCurrentUsages(),
                coupon.getCreatedAt()
        );
    }
}
