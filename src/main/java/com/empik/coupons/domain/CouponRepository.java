package com.empik.coupons.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndCountryCode(String code, String countryCode);

    Page<Coupon> findByCountryCode(String countryCode, Pageable pageable);

    boolean existsByCode(String code);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.currentUsages = c.currentUsages + 1 WHERE c.id = :couponId AND c.currentUsages < c.maxUsages")
    int incrementUsageIfAvailable(@Param("couponId") Long couponId);
}
