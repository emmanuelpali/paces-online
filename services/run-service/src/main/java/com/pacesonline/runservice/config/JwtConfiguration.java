package com.pacesonline.runservice.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
@EnableConfigurationProperties(TokenValidationProperties.class)
public class JwtConfiguration {

    @Bean
    JwtDecoder jwtDecoder(
            TokenValidationProperties properties,
            ResourceLoader resourceLoader,
            UuidJwtSubjectValidator subjectValidator
    ) {
        RSAPublicKey publicKey = readPublicKey(
                resourceLoader.getResource(properties.publicKeyLocation())
        );

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(
                                properties.issuer()
                        ),
                        subjectValidator
                )
        );

        return decoder;
    }

    private RSAPublicKey readPublicKey(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read and configure public key",
                    exception
            );
        }
    }
}