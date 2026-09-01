package com.empik.coupons.api;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.domain.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponRedemptionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/coupons";

    @Autowired
    CouponService couponService;

    @Test
    void redeemCoupon_successfulRedemption() throws Exception {
        couponService.createCoupon("PROMO10", "PL", 5);

        mockMvc.perform(post(BASE + "/PROMO10/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUsages").value(1))
                .andExpect(jsonPath("$.remainingUsages").value(4));
    }

    @Test
    void redeemCoupon_returns404WhenCouponNotFound() throws Exception {
        mockMvc.perform(post(BASE + "/GHOST/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Coupon 'GHOST' not found"));
    }

    @Test
    void redeemCoupon_returns409WhenExhausted() throws Exception {
        couponService.createCoupon("ONE_USE", "PL", 1);
        couponService.redeemCoupon("ONE_USE", "user-1", "PL");

        mockMvc.perform(post(BASE + "/ONE_USE/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-2"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Coupon 'ONE_USE' has reached its maximum usage limit"));
    }

    @Test
    void redeemCoupon_returns409WhenAlreadyUsedByUser() throws Exception {
        couponService.createCoupon("LOYALTY", "PL", 5);
        couponService.redeemCoupon("LOYALTY", "user-1", "PL");

        mockMvc.perform(post(BASE + "/LOYALTY/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("User 'user-1' has already redeemed coupon 'LOYALTY'"));
    }

    @Test
    void redeemCoupon_isCaseInsensitiveForCode() throws Exception {
        couponService.createCoupon("CaseMix", "PL", 5);

        mockMvc.perform(post(BASE + "/casemix/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void redeemCoupon_returns403WhenCountryMismatch() throws Exception {
        couponService.createCoupon("RESTRICTED", "PL", 5);
        stubGeoLocationForCountry("DE");

        mockMvc.perform(post(BASE + "/RESTRICTED/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "user-1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Coupon 'RESTRICTED' is not valid for country 'DE'"));
    }
}
