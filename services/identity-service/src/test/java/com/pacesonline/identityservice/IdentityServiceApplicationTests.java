package com.pacesonline.identityservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.pacesonline.identityservice.auth.login.LoginResult;
import com.pacesonline.identityservice.auth.login.LoginService;
import com.pacesonline.identityservice.auth.registration.RegistrationService;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class IdentityServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginService loginService;

    @Autowired
    private KeyPair jwtKeyPair;

    private String createToken(JwtEncoder encoder, UUID userId,
        String issuer, Instant issuedAt, Instant expiresAt
        ) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .build();

    JwsHeader jwsHeader =
            JwsHeader.with(SignatureAlgorithm.RS256).build();

    return encoder.encode(
            JwtEncoderParameters.from(jwsHeader, claims)
    ).getTokenValue();
}

    @Test
    void contextLoads() {
    }

    @Test
    void flywayCreatesUsersTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'users'
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void registrationPersistsNormalizedEmailAndHashedPassword() {
        User registeredUser = registrationService.register(
                " Runner@Example.com ",
                "strong-password"
        );

        User persistedUser = userRepository.findById(registeredUser.getId())
                .orElseThrow();

        assertThat(persistedUser.getEmail())
                .isEqualTo("runner@example.com");

        assertThat(persistedUser.getPasswordHash())
                .isNotEqualTo("strong-password");

        assertThat(passwordEncoder.matches(
                "strong-password",
                persistedUser.getPasswordHash()
        )).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:17-alpine");
        }
    }

    @Test
    void registeredUserCanLoginAndReceiveVerifiableAccessToken() {
        String email = "login-integration@example.com";
        String password = "strong-password";

        User registeredUser = registrationService.register(
                email,
                password
        );

        LoginResult result = loginService.login(
                email,
                password
        );

        RSAPublicKey publicKey =
                (RSAPublicKey) jwtKeyPair.getPublic();

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        Jwt jwt = jwtDecoder.decode(result.accessToken());

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.expiresIn()).isEqualTo(300);

        assertThat(jwt.getIssuer().toString())
                .isEqualTo("https://identity.pacesonline.test");

        assertThat(jwt.getSubject())
                .isEqualTo(registeredUser.getId().toString());

        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getId()).isNotBlank();
     }

     @Test
     void authenticatedUserCanRetrieveOwnProfile() throws Exception {
        String email = "profile-integration@example.com";
        String password = "strong-password";

        User registeredUser = registrationService.register(
                email,
                password
        );

        LoginResult loginResult = loginService.login(
                email,
                password
        );

        mockMvc.perform(
                        get("/api/v1/users/user")
                                .header(
                                        "Authorization",
                                        "Bearer " + loginResult.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(registeredUser.getId().toString()))
                .andExpect(jsonPath("$.email")
                        .value(email))
                .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        void profileWithoutAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/user")
                )
                .andExpect(status().isUnauthorized());
        }

        @Test
        void malformedAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/user")
                                .header(
                                        "Authorization",
                                        "Bearer not-a-valid-jwt"
                                )
                )
                .andExpect(status().isUnauthorized());
        }

        @Test
        void expiredAccessTokenReturnsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(600);
        Instant expiresAt = Instant.now().minusSeconds(300);

        String token = createToken(
                jwtEncoder,
                UUID.randomUUID(),
                "https://identity.pacesonline.test",
                issuedAt,
                expiresAt
        );

        mockMvc.perform(
                        get("/api/v1/users/user")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
        }

        @Test
        void accessTokenWithWrongIssuerReturnsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);

        String token = createToken(
                jwtEncoder,
                UUID.randomUUID(),
                "https://untrusted.example.com",
                issuedAt,
                expiresAt
        );

        mockMvc.perform(
                        get("/api/v1/users/user")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
        }

        @Test
        void accessTokenSignedByUntrustedKeyReturnsUnauthorized()
                throws Exception {

        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair untrustedKeyPair =
                keyPairGenerator.generateKeyPair();

        RSAPublicKey publicKey =
                (RSAPublicKey) untrustedKeyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) untrustedKeyPair.getPrivate();

        JwtEncoder untrustedEncoder =
                NimbusJwtEncoder
                        .withKeyPair(publicKey, privateKey)
                        .build();

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);

        String token = createToken(
                untrustedEncoder,
                UUID.randomUUID(),
                "https://identity.pacesonline.test",
                issuedAt,
                expiresAt
        );

        mockMvc.perform(
                        get("/api/v1/users/user")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
        }
}