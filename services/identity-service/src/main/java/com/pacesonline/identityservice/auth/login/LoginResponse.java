package com.pacesonline.identityservice.auth.login;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}