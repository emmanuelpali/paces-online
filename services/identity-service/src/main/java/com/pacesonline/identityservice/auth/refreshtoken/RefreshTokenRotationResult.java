package com.pacesonline.identityservice.auth.refreshtoken;

import java.time.Instant;

public record RefreshTokenRotationResult(
    String accessToken,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
}
