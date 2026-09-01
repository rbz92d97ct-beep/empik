package com.empik.coupons.exception;

import com.empik.coupons.infrastructure.geolocation.GeoLocationResolutionException;
import com.empik.coupons.infrastructure.geolocation.GeoLocationUnavailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponExceptions.CouponNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCouponNotFound(CouponExceptions.CouponNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(CouponExceptions.CouponExhaustedException.class)
    public ResponseEntity<ProblemDetail> handleCouponExhausted(CouponExceptions.CouponExhaustedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(CouponExceptions.CouponCountryMismatchException.class)
    public ResponseEntity<ProblemDetail> handleCountryMismatch(CouponExceptions.CouponCountryMismatchException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(problem(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(CouponExceptions.CouponAlreadyUsedException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyUsed(CouponExceptions.CouponAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(CouponExceptions.DuplicateCouponCodeException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateCode(CouponExceptions.DuplicateCouponCodeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(CouponExceptions.InvalidMaxUsagesException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMaxUsages(CouponExceptions.InvalidMaxUsagesException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            int dot = path.lastIndexOf('.');
                            return dot >= 0 ? path.substring(dot + 1) : path;
                        },
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(GeoLocationUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleGeoLocationUnavailable(GeoLocationUnavailableException ex) {
        log.error("GeoLocation provider unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, "Geolocation service is temporarily unavailable"));
    }

    @ExceptionHandler(GeoLocationResolutionException.class)
    public ResponseEntity<ProblemDetail> handleGeoLocationResolution(GeoLocationResolutionException ex) {
        log.warn("GeoLocation resolution failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, "Could not determine request origin"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }
}
