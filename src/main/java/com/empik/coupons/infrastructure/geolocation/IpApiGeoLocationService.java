package com.empik.coupons.infrastructure.geolocation;

import com.empik.coupons.domain.GeoLocationService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;

@Slf4j
@Service
class IpApiGeoLocationService implements GeoLocationService {

    private final RestClient restClient;

    IpApiGeoLocationService(GeoLocationProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Cacheable(value = "geolocation", key = "#ipAddress")
    @Retry(name = "geoLocation")
    @Override
    public String resolveCountryCode(String ipAddress) {
        try {
            IpApiResponse response = restClient.get()
                    .uri("/{ip}?fields=status,countryCode,message", ipAddress)
                    .retrieve()
                    .body(IpApiResponse.class);

            return extractCountryCode(ipAddress, response);

        } catch (RestClientException ex) {
            throw new GeoLocationUnavailableException(
                    "Failed to resolve country for IP " + ipAddress, ex);
        }
    }

    private String extractCountryCode(String ipAddress, IpApiResponse response) {
        if (response == null) {
            throw new GeoLocationUnavailableException("GeoLocation API returned empty response");
        }
        if (!"success".equals(response.status())) {
            throw new GeoLocationResolutionException(ipAddress, response.message());
        }
        String countryCode = response.countryCode();
        if (countryCode == null || countryCode.isBlank()) {
            throw new GeoLocationUnavailableException(
                    "GeoLocation API returned missing country code for IP " + ipAddress);
        }
        log.debug("Resolved country {} for IP {}", countryCode, ipAddress);
        return countryCode.toUpperCase(Locale.ROOT);
    }

    record IpApiResponse(String status, String countryCode, String message) {}
}
