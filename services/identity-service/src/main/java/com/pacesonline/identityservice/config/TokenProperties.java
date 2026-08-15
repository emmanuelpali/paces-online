package com.pacesonline.identityservice.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "paces-online.security.token")
public record TokenProperties(
        @NotBlank String issuer,
        @NotNull Duration accessTokenExpiration,
        @NotNull Duration refreshTokenExpiration,
        String privateKeyLocation,
        String publicKeyLocation
) {

    public TokenProperties {
        if (accessTokenExpiration != null
                && (accessTokenExpiration.isZero() || accessTokenExpiration.isNegative())) {
            throw new IllegalArgumentException(
                    "Access-token expiration must be greater than zero"
            );
        }

        if (refreshTokenExpiration != null
                && (refreshTokenExpiration.isZero() || refreshTokenExpiration.isNegative())) {
            throw new IllegalArgumentException(
                    "Refresh-token expiration must be greater than zero"
            );
        }

        if (accessTokenExpiration != null
                && refreshTokenExpiration != null
                && refreshTokenExpiration.compareTo(accessTokenExpiration) <= 0) {
            throw new IllegalArgumentException(
                    "Refresh-token expiration must be longer than access-token expiration"
            );
        }
    }
}