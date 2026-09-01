package com.empik.coupons.domain;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.exception.CouponExceptions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

class CouponServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CouponService couponService;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponUsageRepository couponUsageRepository;

    @Test
    void createCoupon_savesAndReturnsCoupon() {
        Coupon result = couponService.createCoupon("SUMMER20", "PL", 10);

        assertThat(result.getCode()).isEqualTo("SUMMER20");
        assertThat(result.getCountryCode()).isEqualTo("PL");
        assertThat(result.getMaxUsages()).isEqualTo(10);
        assertThat(result.getCurrentUsages()).isZero();
        assertThat(couponRepository.findByCodeAndCountryCode("SUMMER20", "PL")).isPresent();
    }

    @Test
    void createCoupon_normalizesCodeAndCountry() {
        Coupon result = couponService.createCoupon("summer20", "pl", 5);

        assertThat(result.getCode()).isEqualTo("SUMMER20");
        assertThat(result.getCountryCode()).isEqualTo("PL");
    }

    @Test
    void createCoupon_throwsDuplicateWhenSameCodeAndCountryAlreadyExists() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        assertThatThrownBy(() -> couponService.createCoupon("SUMMER20", "PL", 5))
                .isInstanceOf(DuplicateCouponCodeException.class);
    }

    @Test
    void createCoupon_allowsSameCodeForDifferentCountries() {
        couponService.createCoupon("SUMMER20", "PL", 10);
        couponService.createCoupon("SUMMER20", "DE", 5);

        assertThat(couponRepository.findByCodeAndCountryCode("SUMMER20", "PL")).isPresent();
        assertThat(couponRepository.findByCodeAndCountryCode("SUMMER20", "DE")).isPresent();
    }

    @Test
    void redeemCoupon_successfulRedemption() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        Coupon result = couponService.redeemCoupon("SUMMER20", "user-1", "PL");

        assertThat(result.getCurrentUsages()).isEqualTo(1);
        assertThat(couponUsageRepository.findAll()).hasSize(1);
    }

    @Test
    void redeemCoupon_normalizesCodeAndCountry() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        Coupon result = couponService.redeemCoupon("summer20", "user-1", "pl");

        assertThat(result.getCurrentUsages()).isEqualTo(1);
    }

    @Test
    void redeemCoupon_throwsWhenCouponNotFound() {
        assertThatThrownBy(() -> couponService.redeemCoupon("INVALID", "user-1", "PL"))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void redeemCoupon_throwsWhenCountryMismatch() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        assertThatThrownBy(() -> couponService.redeemCoupon("SUMMER20", "user-1", "DE"))
                .isInstanceOf(CouponCountryMismatchException.class);
    }

    @Test
    void redeemCoupon_throwsWhenCouponExhausted() {
        couponService.createCoupon("SUMMER20", "PL", 1);
        couponService.redeemCoupon("SUMMER20", "user-1", "PL");

        assertThatThrownBy(() -> couponService.redeemCoupon("SUMMER20", "user-2", "PL"))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    void redeemCoupon_throwsWhenAlreadyUsedBySameUser() {
        couponService.createCoupon("SUMMER20", "PL", 10);
        couponService.redeemCoupon("SUMMER20", "user-1", "PL");

        assertThatThrownBy(() -> couponService.redeemCoupon("SUMMER20", "user-1", "PL"))
                .isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    void getCoupon_returnsExistingCoupon() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        Coupon result = couponService.getCoupon("SUMMER20", "PL");

        assertThat(result.getCode()).isEqualTo("SUMMER20");
        assertThat(result.getCountryCode()).isEqualTo("PL");
    }

    @Test
    void getCoupon_throwsWhenNotFound() {
        assertThatThrownBy(() -> couponService.getCoupon("GHOST", "PL"))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void updateMaxUsages_updatesSuccessfully() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        Coupon result = couponService.updateMaxUsages("SUMMER20", "PL", 20);

        assertThat(result.getMaxUsages()).isEqualTo(20);
    }

    @Test
    void updateMaxUsages_throwsWhenBelowCurrentUsages() {
        couponService.createCoupon("SUMMER20", "PL", 5);
        couponService.redeemCoupon("SUMMER20", "user-1", "PL");
        couponService.redeemCoupon("SUMMER20", "user-2", "PL");

        assertThatThrownBy(() -> couponService.updateMaxUsages("SUMMER20", "PL", 1))
                .isInstanceOf(InvalidMaxUsagesException.class);
    }

    @Test
    void deleteCoupon_deletesSuccessfully() {
        couponService.createCoupon("SUMMER20", "PL", 10);

        couponService.deleteCoupon("SUMMER20", "PL");

        assertThat(couponRepository.findByCodeAndCountryCode("SUMMER20", "PL")).isEmpty();
    }

    @Test
    void deleteCoupon_throwsWhenNotFound() {
        assertThatThrownBy(() -> couponService.deleteCoupon("GHOST", "PL"))
                .isInstanceOf(CouponNotFoundException.class);
    }
}
