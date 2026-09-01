package com.empik.coupons.api;

import com.empik.coupons.api.dto.CouponResponse;
import com.empik.coupons.api.dto.CreateCouponRequest;
import com.empik.coupons.api.dto.UpdateCouponRequest;
import com.empik.coupons.api.validation.ValidCountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api/v1/management/coupons")
public class CouponManagementController {

    private final CouponService couponService;

    public CouponManagementController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = couponService.createCoupon(request.code(), request.countryCode(), request.maxUsages());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .queryParam("countryCode", coupon.getCountryCode())
                .buildAndExpand(coupon.getCode())
                .toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @GetMapping
    public Page<CouponResponse> listCoupons(
            @RequestParam @NotBlank @ValidCountryCode String countryCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return couponService.listCoupons(countryCode, pageable).map(CouponResponse::from);
    }

    @GetMapping("/{code}")
    public CouponResponse getCoupon(
            @PathVariable String code,
            @RequestParam @NotBlank @ValidCountryCode String countryCode) {
        return CouponResponse.from(couponService.getCoupon(code, countryCode));
    }

    @PatchMapping("/{code}")
    public CouponResponse updateCoupon(
            @PathVariable String code,
            @RequestParam @NotBlank @ValidCountryCode String countryCode,
            @Valid @RequestBody UpdateCouponRequest request) {
        return CouponResponse.from(couponService.updateMaxUsages(code, countryCode, request.maxUsages()));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable String code,
            @RequestParam @NotBlank @ValidCountryCode String countryCode) {
        couponService.deleteCoupon(code, countryCode);
        return ResponseEntity.noContent().build();
    }
}
