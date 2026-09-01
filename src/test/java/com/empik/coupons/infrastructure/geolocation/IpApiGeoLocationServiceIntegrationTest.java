package com.empik.coupons.infrastructure.geolocation;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.domain.GeoLocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpApiGeoLocationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    GeoLocationService geoLocationService;

    @Test
    void resolveCountryCode_returnsCountryForValidIp() {
        String country = geoLocationService.resolveCountryCode("8.8.8.8");

        assertThat(country).isEqualTo("PL");
        wireMock.verify(getRequestedFor(urlPathMatching("/json/.*")));
    }

    @Test
    void resolveCountryCode_throwsResolutionExceptionWhenApiFails() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .atPriority(1)
                .willReturn(okJson("""
                        {"status":"fail","countryCode":null,"message":"invalid query"}
                        """)));

        assertThatThrownBy(() -> geoLocationService.resolveCountryCode("999.999.999.999"))
                .isInstanceOf(GeoLocationResolutionException.class)
                .hasMessageContaining("invalid query");
    }

    @Test
    void resolveCountryCode_throwsUnavailableWhenCountryCodeMissing() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .atPriority(1)
                .willReturn(okJson("""
                        {"status":"success","countryCode":null,"message":""}
                        """)));

        assertThatThrownBy(() -> geoLocationService.resolveCountryCode("1.2.3.4"))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("missing country code");
    }

    @Test
    void resolveCountryCode_throwsUnavailableOnHttpError() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .atPriority(1)
                .willReturn(serverError()));

        assertThatThrownBy(() -> geoLocationService.resolveCountryCode("1.2.3.4"))
                .isInstanceOf(GeoLocationUnavailableException.class);
    }

    @Test
    void resolveCountryCode_normalizesCountryCodeToUpperCase() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .atPriority(1)
                .willReturn(okJson("""
                        {"status":"success","countryCode":"pl","message":""}
                        """)));

        String country = geoLocationService.resolveCountryCode("1.2.3.4");

        assertThat(country).isEqualTo("PL");
    }

    @Test
    void resolveCountryCode_retriesOnTransientErrorAndSucceeds() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .inScenario("retry")
                .atPriority(1)
                .whenScenarioStateIs(STARTED)
                .willReturn(serverError())
                .willSetStateTo("first-retry"));

        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .inScenario("retry")
                .atPriority(1)
                .whenScenarioStateIs("first-retry")
                .willReturn(serverError())
                .willSetStateTo("second-retry"));

        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .inScenario("retry")
                .atPriority(1)
                .whenScenarioStateIs("second-retry")
                .willReturn(okJson("""
                        {"status":"success","countryCode":"PL","message":""}
                        """)));

        String country = geoLocationService.resolveCountryCode("2.3.4.5");

        assertThat(country).isEqualTo("PL");
        wireMock.verify(3, getRequestedFor(urlPathMatching("/json/2.3.4.5")));
    }

    @Test
    void resolveCountryCode_cachesResultForSameIp() {
        String ip = "5.6.7.8";

        geoLocationService.resolveCountryCode(ip);
        geoLocationService.resolveCountryCode(ip);

        wireMock.verify(1, getRequestedFor(urlPathMatching("/json/" + ip)));
    }
}
