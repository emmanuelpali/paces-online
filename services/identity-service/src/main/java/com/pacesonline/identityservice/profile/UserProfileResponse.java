package com.pacesonline.identityservice.profile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        Instant createdAt
) {
}