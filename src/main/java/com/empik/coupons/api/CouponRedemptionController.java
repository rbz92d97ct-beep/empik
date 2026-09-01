package com.empik.coupons.api;

import com.empik.coupons.api.dto.CouponResponse;
import com.empik.coupons.api.dto.RedeemCouponRequest;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponRedemptionService;
import com.empik.coupons.infrastructure.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponRedemptionController {

    private final CouponRedemptionService couponRedemptionService;
    private final ClientIpResolver clientIpResolver;

    public CouponRedemptionController(CouponRedemptionService couponRedemptionService,
                                      ClientIpResolver clientIpResolver) {
        this.couponRedemptionService = couponRedemptionService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<CouponResponse> redeemCoupon(
            @PathVariable String code,
            @Valid @RequestBody RedeemCouponRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        Coupon coupon = couponRedemptionService.redeem(code, request.userId(), clientIp);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }
}
