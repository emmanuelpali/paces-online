package com.pacesonline.identityservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.pacesonline.identityservice.auth.login.LoginResult;
import com.pacesonline.identityservice.auth.login.LoginService;
import com.pacesonline.identityservice.auth.registration.RegistrationService;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

@ActiveProfiles("test")
@SpringBootTest
class IdentityServiceApplicationTests {

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
}