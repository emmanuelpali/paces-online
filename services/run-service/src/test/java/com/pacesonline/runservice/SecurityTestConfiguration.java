package com.pacesonline.runservice;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.pacesonline.runservice.config.UuidJwtSubjectValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@TestConfiguration(proxyBeanMethods = false)
class SecurityTestConfiguration {

    static final String TEST_ISSUER =
            "https://identity.pacesonline.test";

    @Bean
    KeyPair testJwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    @Bean
    JwtDecoder testJwtDecoder(
            KeyPair testJwtKeyPair,
            UuidJwtSubjectValidator subjectValidator
    ) {
        RSAPublicKey publicKey =
                (RSAPublicKey) testJwtKeyPair.getPublic();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(TEST_ISSUER),
                        subjectValidator
                )
        );

        return decoder;
    }

    @Bean
    JwtEncoder testJwtEncoder(KeyPair testJwtKeyPair) {
        RSAPublicKey publicKey =
                (RSAPublicKey) testJwtKeyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) testJwtKeyPair.getPrivate();

        return NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .build();
    }
}