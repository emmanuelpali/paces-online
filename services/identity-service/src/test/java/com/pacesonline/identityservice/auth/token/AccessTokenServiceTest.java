package com.pacesonline.identityservice.auth.token;

import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenServiceTest {

    private AccessTokenService accessTokenService;
    private NimbusJwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        JwtEncoder jwtEncoder = NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .build();

        jwtDecoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        TokenProperties tokenProperties = new TokenProperties(
                "https://identity.pacesonline.local",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                null,
                null
        );

        accessTokenService =
                new AccessTokenService(jwtEncoder, tokenProperties);
    }

    @Test
    void generatesVerifiableAccessTokenWithExpectedClaims() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "runner@example.com",
                "irrelevant-for-this-test"
        );

        String accessToken =
                accessTokenService.generateAccessToken(user);

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(jwt.getIssuer().toString())
                .isEqualTo("https://identity.pacesonline.local");

        assertThat(jwt.getSubject())
                .isEqualTo(userId.toString());

        assertThat(jwt.getIssuedAt())
                .isNotNull();

        assertThat(jwt.getExpiresAt())
                .isNotNull();

        assertThat(jwt.getId())
                .isNotBlank();

        assertThat(jwt.getExpiresAt())
                .isAfter(jwt.getIssuedAt());
    }
}