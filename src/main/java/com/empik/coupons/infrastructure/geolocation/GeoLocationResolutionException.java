package com.empik.coupons.infrastructure.geolocation;

public class GeoLocationResolutionException extends RuntimeException {

    public GeoLocationResolutionException(String ipAddress, String reason) {
        super("Could not resolve country for IP %s: %s".formatted(ipAddress, reason));
    }
}
