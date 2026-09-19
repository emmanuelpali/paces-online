package com.pacesonline.runservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "runs")
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 20)
    private RunType runType;

    @Column(name = "distance_kilometres", nullable = false, precision = 7, scale = 3)
    private BigDecimal distanceKilometres;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "average_pace_seconds_per_kilometre", nullable = false)
    private Integer averagePaceSecondsPerKilometre;

    @Column(name = "perceived_effort")
    private Short perceivedEffort;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Run() {
        // Required by JPA.
    }

    public Run(
            UUID userId,
            Instant startedAt,
            RunType runType,
            BigDecimal distanceKilometres,
            Integer durationSeconds,
            Integer averagePaceSecondsPerKilometre,
            Short perceivedEffort,
            String notes) {
        this.userId = userId;
        this.startedAt = startedAt;
        this.runType = runType;
        this.distanceKilometres = distanceKilometres;
        this.durationSeconds = durationSeconds;
        this.averagePaceSecondsPerKilometre = averagePaceSecondsPerKilometre;
        this.perceivedEffort = perceivedEffort;
        this.notes = notes;
    }

    @PrePersist
    private void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = Instant.now();
    }

}