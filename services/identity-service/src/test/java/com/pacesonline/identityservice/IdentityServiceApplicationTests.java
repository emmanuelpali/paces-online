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
import com.pacesonline.identityservice.auth.refreshtoken.RefreshTokenGenerator;
import com.pacesonline.identityservice.auth.registration.RegistrationService;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import com.pacesonline.identityservice.auth.refreshtoken.InvalidRefreshTokenException;
import com.pacesonline.identityservice.auth.refreshtoken.RefreshTokenRotationResult;
import com.pacesonline.identityservice.auth.refreshtoken.RefreshTokenService;

import com.pacesonline.identityservice.auth.login.InvalidCredentialsException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Autowired
    private RefreshTokenGenerator refreshTokenGenerator;

    @Autowired
    private RefreshTokenService refreshTokenService;

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
    void flywayCreatesRefreshTokensTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_name = 'refresh_tokens'
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
    void registeredUserCanLoginAndReceiveTokenPair() {
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
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.accessTokenExpiresIn()).isEqualTo(300);
        assertThat(result.refreshTokenExpiresIn()).isEqualTo(3600);

        assertThat(jwt.getIssuer().toString())
                .isEqualTo("https://identity.pacesonline.test");

        assertThat(jwt.getSubject())
                .isEqualTo(registeredUser.getId().toString());

        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getId()).isNotBlank();

        String persistedTokenHash = jdbcTemplate.queryForObject(
                """
                SELECT token_hash
                FROM refresh_tokens
                WHERE user_id = ?
                """,
                String.class,
                registeredUser.getId()
        );

        assertThat(persistedTokenHash).isNotBlank();
        assertThat(persistedTokenHash).hasSize(64);
        assertThat(persistedTokenHash)
                .isNotEqualTo(result.refreshToken());

        assertThat(persistedTokenHash).isEqualTo(
                refreshTokenGenerator.hash(result.refreshToken())
        );
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

        @Test
        void validRefreshTokenRotatesWithinSameFamily() {
        String email = "refresh-integration@example.com";
        String password = "strong-password";

        User registeredUser = registrationService.register(
                email,
                password
        );

        LoginResult loginResult = loginService.login(
                email,
                password
        );

        String originalHash = refreshTokenGenerator.hash(
                loginResult.refreshToken()
        );

        UUID familyId = jdbcTemplate.queryForObject(
                """
                SELECT family_id
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                UUID.class,
                originalHash
        );

        RefreshTokenRotationResult rotationResult =
                refreshTokenService.rotate(
                        loginResult.refreshToken()
                );

        String replacementHash = refreshTokenGenerator.hash(
                rotationResult.refreshToken()
        );

        assertThat(rotationResult.accessToken()).isNotBlank();
        assertThat(rotationResult.refreshToken()).isNotBlank();
        assertThat(rotationResult.refreshToken())
                .isNotEqualTo(loginResult.refreshToken());

        assertThat(rotationResult.accessTokenExpiresIn())
                .isEqualTo(300);

        assertThat(rotationResult.refreshTokenExpiresIn())
                .isBetween(1L, 3600L);

        Boolean originalWasConsumed = jdbcTemplate.queryForObject(
                """
                SELECT consumed_at IS NOT NULL
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                Boolean.class,
                originalHash
        );

        UUID replacementFamilyId = jdbcTemplate.queryForObject(
                """
                SELECT family_id
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                UUID.class,
                replacementHash
        );

        Long familyTokenCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_tokens
                WHERE family_id = ?
                """,
                Long.class,
                familyId
        );

        Long activeTokenCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_tokens
                WHERE family_id = ?
                AND consumed_at IS NULL
                AND revoked_at IS NULL
                """,
                Long.class,
                familyId
        );

        Long distinctExpirationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT expires_at)
                FROM refresh_tokens
                WHERE family_id = ?
                """,
                Long.class,
                familyId
        );

        assertThat(originalWasConsumed).isTrue();
        assertThat(replacementFamilyId).isEqualTo(familyId);
        assertThat(familyTokenCount).isEqualTo(2);
        assertThat(activeTokenCount).isEqualTo(1);
        assertThat(distinctExpirationCount).isEqualTo(1);

        assertThat(replacementHash)
                .isNotEqualTo(rotationResult.refreshToken());
        }


        @Test
        void reusingConsumedRefreshTokenRevokesFamily() {
        String email = "refresh-reuse@example.com";
        String password = "strong-password";

        registrationService.register(email, password);

        LoginResult loginResult = loginService.login(
                email,
                password
        );

        String originalHash = refreshTokenGenerator.hash(
                loginResult.refreshToken()
        );

        UUID familyId = jdbcTemplate.queryForObject(
                """
                SELECT family_id
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                UUID.class,
                originalHash
        );

        RefreshTokenRotationResult rotationResult =
                refreshTokenService.rotate(
                        loginResult.refreshToken()
                );

        assertThatThrownBy(() ->
                refreshTokenService.rotate(
                        loginResult.refreshToken()
                )
        ).isInstanceOf(InvalidRefreshTokenException.class);

        String replacementHash = refreshTokenGenerator.hash(
                rotationResult.refreshToken()
        );

        Boolean replacementWasRevoked =
                jdbcTemplate.queryForObject(
                        """
                        SELECT revoked_at IS NOT NULL
                        FROM refresh_tokens
                        WHERE token_hash = ?
                        """,
                        Boolean.class,
                        replacementHash
                );

        Long unrevokedFamilyTokenCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM refresh_tokens
                        WHERE family_id = ?
                        AND revoked_at IS NULL
                        """,
                        Long.class,
                        familyId
                );

        assertThat(replacementWasRevoked).isTrue();
        assertThat(unrevokedFamilyTokenCount).isZero();

        assertThatThrownBy(() ->
                refreshTokenService.rotate(
                        rotationResult.refreshToken()
                )
        ).isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        void concurrentRefreshRequestsAllowExactlyOneRotation()
                throws Exception {

        String email = "refresh-concurrent@example.com";
        String password = "strong-password";

        registrationService.register(email, password);

        LoginResult loginResult = loginService.login(
                email,
                password
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        Callable<Boolean> rotate = () -> {
                ready.countDown();
                start.await();

                try {
                refreshTokenService.rotate(
                        loginResult.refreshToken()
                );

                return true;
                } catch (InvalidRefreshTokenException exception) {
                return false;
                }
        };

        try {
                Future<Boolean> firstAttempt =
                        executor.submit(rotate);

                Future<Boolean> secondAttempt =
                        executor.submit(rotate);

                assertThat(
                        ready.await(5, TimeUnit.SECONDS)
                ).isTrue();

                start.countDown();

                List<Boolean> results = List.of(
                        firstAttempt.get(10, TimeUnit.SECONDS),
                        secondAttempt.get(10, TimeUnit.SECONDS)
                );

                assertThat(results)
                        .containsExactlyInAnyOrder(true, false);
        } finally {
                // Prevent waiting threads from remaining blocked if an
                // assertion or submission fails before the start signal.
                start.countDown();
                executor.shutdownNow();
        }

        String originalHash = refreshTokenGenerator.hash(
                loginResult.refreshToken()
        );

        UUID familyId = jdbcTemplate.queryForObject(
                """
                SELECT family_id
                FROM refresh_tokens
                WHERE token_hash = ?
                """,
                UUID.class,
                originalHash
        );

        Long activeTokenCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_tokens
                WHERE family_id = ?
                AND consumed_at IS NULL
                AND revoked_at IS NULL
                """,
                Long.class,
                familyId
        );

        assertThat(activeTokenCount).isZero();
        }

        @Test
        void expiredPersistedRefreshTokenCannotBeRotated() {
        String email = "refresh-expired@example.com";
        String password = "strong-password";

        registrationService.register(email, password);

        LoginResult loginResult = loginService.login(
                email,
                password
        );

        String tokenHash = refreshTokenGenerator.hash(
                loginResult.refreshToken()
        );

        jdbcTemplate.update(
                """
                UPDATE refresh_tokens
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                        expires_at = CURRENT_TIMESTAMP - INTERVAL '1 hour'
                WHERE token_hash = ?
                """,
                tokenHash
        );

        assertThatThrownBy(() ->
                refreshTokenService.rotate(
                        loginResult.refreshToken()
                )
        ).isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        void unsuccessfulLoginDoesNotPersistRefreshToken() {
        String email = "refresh-failed-login@example.com";
        String password = "strong-password";

        User user = registrationService.register(
                email,
                password
        );

        assertThatThrownBy(() ->
                loginService.login(
                        email,
                        "incorrect-password"
                )
        ).isInstanceOf(InvalidCredentialsException.class);

        Long refreshTokenCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM refresh_tokens
                WHERE user_id = ?
                """,
                Long.class,
                user.getId()
        );

        assertThat(refreshTokenCount).isZero();
        }
}