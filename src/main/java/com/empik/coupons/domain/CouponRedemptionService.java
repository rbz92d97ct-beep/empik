package com.empik.coupons.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponRedemptionService {

    private final GeoLocationService geoLocationService;
    private final CouponService couponService;

    public Coupon redeem(String code, String userId, String clientIp) {
        String countryCode = geoLocationService.resolveCountryCode(clientIp);
        return couponService.redeemCoupon(code, userId, countryCode);
    }
}
