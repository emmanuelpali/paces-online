package com.pacesonline.identityservice.auth.login;

public record LoginResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}