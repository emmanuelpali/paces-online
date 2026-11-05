package com.pacesonline.runservice.config;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class UuidJwtSubjectValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_SUBJECT = new OAuth2Error(
            "invalid_token",
            "JWT subject must be a UUID",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            return OAuth2TokenValidatorResult.failure(INVALID_SUBJECT);
        }

        try {
            UUID.fromString(subject);
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_SUBJECT);
        }
    }
}