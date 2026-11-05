package com.pacesonline.runservice;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        SecurityTestConfiguration.class,
        RunSecurityTests.ProtectedTestController.class
})
class RunSecurityTests {

    private static final String PROTECTED_PATH = "/api/test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void rejectsProtectedRequestWithoutAccessToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsProtectedRequestWithValidAccessToken() throws Exception {
        Instant now = Instant.now();

        String accessToken = encodeToken(
                jwtEncoder,
                SecurityTestConfiguration.TEST_ISSUER,
                UUID.randomUUID().toString(),
                now,
                now.plusSeconds(300)
        );

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("authenticated"));
    }

    @Test
    void rejectsExpiredAccessToken() throws Exception {
        Instant now = Instant.now();

        String accessToken = encodeToken(
                jwtEncoder,
                SecurityTestConfiguration.TEST_ISSUER,
                UUID.randomUUID().toString(),
                now.minusSeconds(600),
                now.minusSeconds(300)
        );

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessTokenWithWrongIssuer() throws Exception {
        Instant now = Instant.now();

        String accessToken = encodeToken(
                jwtEncoder,
                "https://untrusted-issuer.example",
                UUID.randomUUID().toString(),
                now,
                now.plusSeconds(300)
        );

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessTokenWithInvalidSubject() throws Exception {
        Instant now = Instant.now();

        String accessToken = encodeToken(
                jwtEncoder,
                SecurityTestConfiguration.TEST_ISSUER,
                "not-a-uuid",
                now,
                now.plusSeconds(300)
        );

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessTokenSignedByUntrustedKey() throws Exception {
        JwtEncoder untrustedEncoder = createUntrustedEncoder();
        Instant now = Instant.now();

        String accessToken = encodeToken(
                untrustedEncoder,
                SecurityTestConfiguration.TEST_ISSUER,
                UUID.randomUUID().toString(),
                now,
                now.plusSeconds(300)
        );

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isUnauthorized());
    }

    private String encodeToken(
            JwtEncoder encoder,
            String issuer,
            String subject,
            Instant issuedAt,
            Instant expiresAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .build();

        return encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    private JwtEncoder createUntrustedEncoder() throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        return NimbusJwtEncoder.withKeyPair(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate()
        ).build();
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping(PROTECTED_PATH)
        String protectedEndpoint() {
            return "authenticated";
        }
    }
}