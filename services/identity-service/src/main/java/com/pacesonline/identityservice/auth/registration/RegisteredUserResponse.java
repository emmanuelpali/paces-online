package com.pacesonline.identityservice.auth.registration;

import java.time.Instant;
import java.util.UUID;

public record RegisteredUserResponse(
        UUID id,
        String email,
        Instant createdAt
) {
}