package com.pacesonline.runservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.pacesonline.runservice.entity.RunType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRunRequest(
        @NotNull
        Instant startedAt,

        @NotNull
        RunType runType,

        @NotNull
        @Positive
        @Digits(integer = 4, fraction = 3)
        BigDecimal distanceKilometres,

        @NotNull
        @Positive
        Integer durationSeconds,

        @Min(1)
        @Max(10)
        Short perceivedEffort,

        @Size(max = 1000)
        String notes
) {
}
