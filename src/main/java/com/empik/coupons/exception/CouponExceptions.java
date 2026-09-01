package com.empik.coupons.exception;

public final class CouponExceptions {

    private CouponExceptions() {}

    public static class CouponNotFoundException extends RuntimeException {
        public CouponNotFoundException(String code) {
            super("Coupon '%s' not found".formatted(code));
        }
        public CouponNotFoundException(String code, String countryCode) {
            super("Coupon '%s' for country '%s' not found".formatted(code, countryCode));
        }
    }

    public static class CouponExhaustedException extends RuntimeException {
        public CouponExhaustedException(String code) {
            super("Coupon '%s' has reached its maximum usage limit".formatted(code));
        }
    }

    public static class CouponCountryMismatchException extends RuntimeException {
        public CouponCountryMismatchException(String code, String requestCountry) {
            super("Coupon '%s' is not valid for country '%s'".formatted(code, requestCountry));
        }
    }

    public static class CouponAlreadyUsedException extends RuntimeException {
        public CouponAlreadyUsedException(String code, String userId) {
            super("User '%s' has already redeemed coupon '%s'".formatted(userId, code));
        }
    }

    public static class DuplicateCouponCodeException extends RuntimeException {
        public DuplicateCouponCodeException(String code, String countryCode) {
            super("Coupon with code '%s' for country '%s' already exists".formatted(code, countryCode));
        }
    }

    public static class InvalidMaxUsagesException extends RuntimeException {
        public InvalidMaxUsagesException(String code, int newMax, int currentUsages) {
            super("Cannot set maxUsages to %d for coupon '%s' — already used %d times"
                    .formatted(newMax, code, currentUsages));
        }
    }
}
