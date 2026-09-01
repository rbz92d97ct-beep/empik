package com.empik.coupons.domain;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.exception.CouponExceptions.CouponAlreadyUsedException;
import com.empik.coupons.exception.CouponExceptions.CouponExhaustedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CouponConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    CouponService couponService;

    @Test
    void concurrentRedemptions_neverExceedMaxUsages() throws Exception {
        int maxUsages = 5;
        int threads = 20;
        String code = "RACE-MULTI";

        couponService.createCoupon(code, "PL", maxUsages);

        CountDownLatch readyGate = new CountDownLatch(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                String userId = UUID.randomUUID().toString();
                futures.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    try {
                        couponService.redeemCoupon(code, userId, "PL");
                        return true;
                    } catch (CouponExhaustedException ex) {
                        return false;
                    }
                }));
            }

            assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();

            long successCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            Coupon coupon = couponService.getCoupon(code, "PL");
            assertThat(successCount).isEqualTo(maxUsages);
            assertThat(coupon.getCurrentUsages()).isEqualTo(maxUsages);
        }
    }

    @Test
    void concurrentRedemptions_sameUser_onlyOneSucceeds() throws Exception {
        int threads = 20;
        String code = "RACE-SINGLE";
        String userId = UUID.randomUUID().toString();

        couponService.createCoupon(code, "PL", threads);

        CountDownLatch readyGate = new CountDownLatch(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    try {
                        couponService.redeemCoupon(code, userId, "PL");
                        return true;
                    } catch (CouponAlreadyUsedException ex) {
                        return false;
                    }
                }));
            }

            assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();

            long successCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            Coupon coupon = couponService.getCoupon(code, "PL");
            assertThat(successCount).isEqualTo(1);
            assertThat(coupon.getCurrentUsages()).isEqualTo(1);
        }
    }
}
