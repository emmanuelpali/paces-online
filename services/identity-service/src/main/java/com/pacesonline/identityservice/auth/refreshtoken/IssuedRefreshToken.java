package com.pacesonline.identityservice.auth.refreshtoken;

import java.time.Instant;

public record IssuedRefreshToken(
        String rawToken,
        Instant expiresAt
) {
}
