package com.empik.coupons;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres;
    protected static final WireMockServer wireMock;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();

        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("geolocation.base-url", () -> wireMock.baseUrl() + "/json");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CacheManager cacheManager;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        Optional.ofNullable(cacheManager.getCache("geolocation")).ifPresent(Cache::clear);
        stubGeoLocationForPl();
    }

    @AfterEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    protected void stubGeoLocationForPl() {
        stubGeoLocationForCountry("PL");
    }

    protected void stubGeoLocationForCountry(String countryCode) {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .willReturn(okJson("""
                        {"status":"success","countryCode":"%s","message":""}
                        """.formatted(countryCode))));
    }
}
