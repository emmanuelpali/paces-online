package com.pacesonline.identityservice.auth.login;

public record LoginResult(
        String accessToken,
        long expiresIn
) {
}