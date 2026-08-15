package com.pacesonline.identityservice.auth.login;

import com.pacesonline.identityservice.auth.login.InvalidCredentialsException;
import com.pacesonline.identityservice.auth.login.LoginResult;
import com.pacesonline.identityservice.auth.login.LoginService;
import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccessTokenService accessTokenService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        TokenProperties tokenProperties = new TokenProperties(
                "https://identity.pacesonline.test",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                null,
                null
        );

        loginService = new LoginService(
                userRepository,
                passwordEncoder,
                accessTokenService,
                tokenProperties
        );
    }

    @Test
    void authenticatesValidCredentialsAndReturnsAccessToken() {
        User user = new User(
                UUID.randomUUID(),
                "runner@example.com",
                "stored-password-hash"
        );

        when(userRepository.findByEmail("runner@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "strong-password",
                "stored-password-hash"
        )).thenReturn(true);

        when(accessTokenService.generateAccessToken(user))
                .thenReturn("signed-access-token");

        LoginResult result = loginService.login(
                " Runner@Example.com ",
                "strong-password"
        );

        assertThat(result.accessToken())
                .isEqualTo("signed-access-token");

        assertThat(result.expiresIn())
                .isEqualTo(900);

        verify(userRepository)
                .findByEmail("runner@example.com");

        verify(passwordEncoder)
                .matches(
                        "strong-password",
                        "stored-password-hash"
                );

        verify(accessTokenService)
                .generateAccessToken(user);
    }

    @Test
    void rejectsUnknownEmailWithoutGeneratingAccessToken() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                loginService.login(
                        "unknown@example.com",
                        "strong-password"
                )
        )
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never())
                .matches(any(), any());

        verify(accessTokenService, never())
                .generateAccessToken(any());
    }

    @Test
    void rejectsIncorrectPasswordWithoutGeneratingAccessToken() {
        User user = new User(
                UUID.randomUUID(),
                "runner@example.com",
                "stored-password-hash"
        );

        when(userRepository.findByEmail("runner@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "stored-password-hash"
        )).thenReturn(false);

        assertThatThrownBy(() ->
                loginService.login(
                        "runner@example.com",
                        "wrong-password"
                )
        )
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(accessTokenService, never())
                .generateAccessToken(any());
    }
}