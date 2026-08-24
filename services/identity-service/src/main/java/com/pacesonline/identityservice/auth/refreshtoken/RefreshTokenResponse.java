package com.pacesonline.identityservice.auth.refreshtoken;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}