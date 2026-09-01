package com.empik.coupons.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

    private static final Set<String> VALID_CODES = Set.of(Locale.getISOCountries());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || VALID_CODES.contains(value.toUpperCase(Locale.ROOT));
    }
}
