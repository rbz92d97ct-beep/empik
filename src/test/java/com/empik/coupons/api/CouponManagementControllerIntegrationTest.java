package com.empik.coupons.api;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.domain.CouponService;
import com.empik.coupons.domain.CouponUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CouponManagementControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/management/coupons";

    @Autowired
    CouponService couponService;

    @Autowired
    CouponUsageRepository couponUsageRepository;

    @Test
    void createCoupon_returns201WithLocation() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "SPRING25", "countryCode": "PL", "maxUsages": 100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/v1/management/coupons/SPRING25?countryCode=PL")))
                .andExpect(jsonPath("$.code").value("SPRING25"))
                .andExpect(jsonPath("$.countryCode").value("PL"))
                .andExpect(jsonPath("$.maxUsages").value(100))
                .andExpect(jsonPath("$.currentUsages").value(0))
                .andExpect(jsonPath("$.remainingUsages").value(100));
    }

    @Test
    void createCoupon_returns409WhenDuplicateCodeAndCountry() throws Exception {
        couponService.createCoupon("SUMMER", "PL", 5);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "summer", "countryCode": "PL", "maxUsages": 5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Coupon with code 'SUMMER' for country 'PL' already exists"));
    }

    @Test
    void createCoupon_allowsSameCodeForDifferentCountry() throws Exception {
        couponService.createCoupon("MULTI", "PL", 5);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "MULTI", "countryCode": "DE", "maxUsages": 5}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void createCoupon_returns400WhenInvalidRequest() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "countryCode": "INVALID", "maxUsages": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void getCoupon_returnsDetails() throws Exception {
        couponService.createCoupon("INFO", "PL", 5);

        mockMvc.perform(get(BASE + "/INFO").param("countryCode", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INFO"))
                .andExpect(jsonPath("$.countryCode").value("PL"));
    }

    @Test
    void getCoupon_returns404WhenNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/GHOST").param("countryCode", "PL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Coupon 'GHOST' for country 'PL' not found"));
    }

    @Test
    void updateCoupon_increasesMaxUsages() throws Exception {
        couponService.createCoupon("UPD", "PL", 10);

        mockMvc.perform(patch(BASE + "/UPD")
                        .param("countryCode", "PL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxUsages": 50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxUsages").value(50))
                .andExpect(jsonPath("$.remainingUsages").value(50));
    }

    @Test
    void updateCoupon_returns422WhenBelowCurrentUsages() throws Exception {
        couponService.createCoupon("USED", "PL", 5);
        couponService.redeemCoupon("USED", "user-1", "PL");
        couponService.redeemCoupon("USED", "user-2", "PL");

        mockMvc.perform(patch(BASE + "/USED")
                        .param("countryCode", "PL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxUsages": 1}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("already used 2 times")));
    }

    @Test
    void deleteCoupon_returns204() throws Exception {
        couponService.createCoupon("TODELETE", "PL", 5);

        mockMvc.perform(delete(BASE + "/TODELETE").param("countryCode", "PL"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/TODELETE").param("countryCode", "PL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCoupon_cascadesUsages() throws Exception {
        couponService.createCoupon("CASCADE", "PL", 5);
        couponService.redeemCoupon("CASCADE", "user-1", "PL");

        assertThat(couponUsageRepository.findAll()).hasSize(1);

        mockMvc.perform(delete(BASE + "/CASCADE").param("countryCode", "PL"))
                .andExpect(status().isNoContent());

        assertThat(couponUsageRepository.findAll()).isEmpty();
    }

    @Test
    void deleteCoupon_returns404WhenNotFound() throws Exception {
        mockMvc.perform(delete(BASE + "/GHOST").param("countryCode", "PL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Coupon 'GHOST' for country 'PL' not found"));
    }
}
