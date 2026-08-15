package com.pacesonline.identityservice.auth.login;

import com.pacesonline.identityservice.auth.token.AccessTokenService;
import com.pacesonline.identityservice.config.TokenProperties;
import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final TokenProperties tokenProperties;
    private static final String DUMMY_PASSWORD = "user-not-found-password";

    private final String dummyPasswordHash;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            TokenProperties tokenProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.tokenProperties = tokenProperties;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    public LoginResult login(String email, String password) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String accessToken = accessTokenService.generateAccessToken(user);
        return new LoginResult(accessToken, tokenProperties.accessTokenExpiration().toSeconds());
    }
}