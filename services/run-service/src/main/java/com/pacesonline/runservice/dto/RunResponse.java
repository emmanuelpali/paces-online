package com.pacesonline.runservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.pacesonline.runservice.entity.RunType;

public record RunResponse(
        UUID id,
        UUID userId,
        Instant startedAt,
        RunType runType,
        BigDecimal distanceKilometres,
        Integer durationSeconds,
        Integer averagePaceSecondsPerKilometre,
        Short perceivedEffort,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
