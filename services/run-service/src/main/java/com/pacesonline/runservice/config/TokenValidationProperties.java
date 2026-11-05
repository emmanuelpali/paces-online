package com.pacesonline.runservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "paces-online.security.token")
public record TokenValidationProperties(
        @NotBlank String issuer,
        @NotBlank String publicKeyLocation
) {
}