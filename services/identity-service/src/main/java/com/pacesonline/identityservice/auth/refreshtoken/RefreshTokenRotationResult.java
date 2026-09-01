package com.pacesonline.identityservice.auth.refreshtoken;

public record RefreshTokenRotationResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}