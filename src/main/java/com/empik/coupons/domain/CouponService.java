package com.empik.coupons.domain;

import com.empik.coupons.exception.CouponExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Transactional
    public Coupon createCoupon(String code, String countryCode, int maxUsages) {
        try {
            return couponRepository.saveAndFlush(new Coupon(code, countryCode, maxUsages));
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw new DuplicateCouponCodeException(normalize(code), normalize(countryCode));
            }
            throw ex;
        }
    }

    @Transactional
    public Coupon redeemCoupon(String code, String userId, String countryCode) {
        Coupon coupon = findForRedemption(normalize(code), normalize(countryCode));
        Long couponId = coupon.getId();

        int updated = couponRepository.incrementUsageIfAvailable(couponId);

        if (updated == 0) {
            throw new CouponExhaustedException(coupon.getCode());
        }

        Coupon freshCoupon = couponRepository.findById(couponId).orElseThrow();

        try {
            couponUsageRepository.saveAndFlush(new CouponUsage(freshCoupon, userId));
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw new CouponAlreadyUsedException(freshCoupon.getCode(), userId);
            }
            throw ex;
        }

        log.info("Coupon '{}' redeemed by user '{}'", freshCoupon.getCode(), userId);
        return freshCoupon;
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(String code, String countryCode) {
        String normalizedCode = normalize(code);
        String normalizedCountry = normalize(countryCode);
        return couponRepository.findByCodeAndCountryCode(normalizedCode, normalizedCountry)
                .orElseThrow(() -> new CouponNotFoundException(normalizedCode, normalizedCountry));
    }

    @Transactional(readOnly = true)
    public Page<Coupon> listCoupons(String countryCode, Pageable pageable) {
        return couponRepository.findByCountryCode(normalize(countryCode), pageable);
    }

    @Transactional
    public Coupon updateMaxUsages(String code, String countryCode, int newMaxUsages) {
        Coupon coupon = findOrThrow(normalize(code), normalize(countryCode));
        if (newMaxUsages < coupon.getCurrentUsages()) {
            throw new InvalidMaxUsagesException(coupon.getCode(), newMaxUsages, coupon.getCurrentUsages());
        }
        coupon.updateMaxUsages(newMaxUsages);
        return coupon;
    }

    @Transactional
    public void deleteCoupon(String code, String countryCode) {
        Coupon coupon = findOrThrow(normalize(code), normalize(countryCode));
        couponRepository.delete(coupon);
    }

    private Coupon findForRedemption(String code, String countryCode) {
        Optional<Coupon> couponOpt = couponRepository.findByCodeAndCountryCode(code, countryCode);
        if (couponOpt.isPresent()) {
            return couponOpt.get();
        }
        if (couponRepository.existsByCode(code)) {
            log.warn("Coupon '{}' rejected for country '{}' (exists for a different country)", code, countryCode);
            throw new CouponCountryMismatchException(code, countryCode);
        }
        throw new CouponNotFoundException(code);
    }

    private Coupon findOrThrow(String code, String countryCode) {
        return couponRepository.findByCodeAndCountryCode(code, countryCode)
                .orElseThrow(() -> new CouponNotFoundException(code, countryCode));
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        return cause instanceof SQLException sqlEx
                && UNIQUE_VIOLATION_SQLSTATE.equals(sqlEx.getSQLState());
    }
}
