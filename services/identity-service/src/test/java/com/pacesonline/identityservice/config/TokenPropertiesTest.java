package com.pacesonline.identityservice.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsValidTokenProperties() {
        contextRunner
                .withPropertyValues(
                        "paces-online.security.token.issuer=paces-online-test",
                        "paces-online.security.token.access-token-expiration=15m",
                        "paces-online.security.token.refresh-token-expiration=7d"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    TokenProperties properties =
                            context.getBean(TokenProperties.class);

                    assertThat(properties.issuer())
                            .isEqualTo("paces-online-test");

                    assertThat(properties.accessTokenExpiration())
                            .isEqualTo(Duration.ofMinutes(15));

                    assertThat(properties.refreshTokenExpiration())
                            .isEqualTo(Duration.ofDays(7));
                });
    }

    @Test
    void failsWhenAccessTokenExpirationIsZero() {
        contextRunner
                .withPropertyValues(
                        "paces-online.security.token.issuer=paces-online-test",
                        "paces-online.security.token.access-token-expiration=0m",
                        "paces-online.security.token.refresh-token-expiration=7d"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasRootCauseMessage(
                                    "Access-token expiration must be greater than zero"
                            );
                });
    }

    @Test
    void failsWhenRefreshTokenIsNotLongerThanAccessToken() {
        contextRunner
                .withPropertyValues(
                        "paces-online.security.token.issuer=paces-online-test",
                        "paces-online.security.token.access-token-expiration=15m",
                        "paces-online.security.token.refresh-token-expiration=10m"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasRootCauseMessage(
                                    "Refresh-token expiration must be longer than access-token expiration"
                            );
                });
    }

    @Test
    void failsWhenIssuerIsBlank() {
        contextRunner
                .withPropertyValues(
                        "paces-online.security.token.issuer=",
                        "paces-online.security.token.access-token-expiration=15m",
                        "paces-online.security.token.refresh-token-expiration=7d"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                });
    }

    @Test
    void failsWhenRefreshTokenExpirationIsNegative() {
        contextRunner
                .withPropertyValues(
                        "paces-online.security.token.issuer=paces-online-test",
                        "paces-online.security.token.access-token-expiration=15m",
                        "paces-online.security.token.refresh-token-expiration=-1m"
                )
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasRootCauseMessage(
                                    "Refresh-token expiration must be greater than zero"
                            );
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TokenProperties.class)
    static class TestConfiguration {
    }
}
