package com.pacesonline.identityservice.auth.login;

import com.pacesonline.identityservice.auth.refreshtoken.IssuedRefreshToken;
import com.pacesonline.identityservice.auth.refreshtoken.RefreshTokenService;
import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class LoginService {

    private static final String DUMMY_PASSWORD =
            "user-not-found-password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final TokenProperties tokenProperties;
    private final String dummyPasswordHash;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService,
            TokenProperties tokenProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.tokenProperties = tokenProperties;
        this.dummyPasswordHash =
                passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public LoginResult login(String email, String password) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(
                password,
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        String accessToken =
                accessTokenService.generateAccessToken(user);

        IssuedRefreshToken refreshToken =
                refreshTokenService.issueNewFamily(user);

        return new LoginResult(
                accessToken,
                refreshToken.rawToken(),
                tokenProperties.accessTokenExpiration().toSeconds(),
                tokenProperties.refreshTokenExpiration().toSeconds()
        );
    }
}