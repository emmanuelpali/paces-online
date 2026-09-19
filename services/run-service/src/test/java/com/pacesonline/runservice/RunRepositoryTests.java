package com.pacesonline.runservice;

import com.pacesonline.runservice.entity.Run;
import com.pacesonline.runservice.entity.RunType;
import com.pacesonline.runservice.repository.RunRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RunRepositoryTests {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private EntityManager entityManager;
    private Run runForTest(
        RunType type,
        int paceSeconds,
        Short effort,
        String notes
        ) {
        return new Run(
                UUID.randomUUID(),
                Instant.parse("2026-09-17T10:00:00Z"),
                type,
                new BigDecimal("5.000"),
                1500,
                paceSeconds,
                effort,
                notes
        );
    } 

    @Test
    void persistsAndReadsRun() {
        // Arrange: valid data with predictable values.
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-17T10:00:00Z");

        Run run = new Run(
                userId,
                startedAt,
                RunType.EASY,
                new BigDecimal("5.000"),
                1500,
                300,
                (short) 4,
                "Comfortable morning run");

        // Act: write to PostgreSQL, then force a fresh read.
        Run saved = runRepository.saveAndFlush(run);
        UUID runId = saved.getId();

        entityManager.clear();

        Run reloaded = runRepository.findById(runId)
                .orElseThrow();

        // Assert: verify values loaded from PostgreSQL.
        assertThat(reloaded.getRunType()).isEqualTo(RunType.EASY);

        assertThat(reloaded.getDistanceKilometres())
                .isEqualByComparingTo("5.000");

        assertThat(reloaded.getId())
                .isNotNull()
                .isEqualTo(runId);

        assertThat(reloaded.getUserId()).isEqualTo(userId);
        assertThat(reloaded.getStartedAt()).isEqualTo(startedAt);
        assertThat(reloaded.getDurationSeconds()).isEqualTo(1500);
        assertThat(reloaded.getAveragePaceSecondsPerKilometre())
                .isEqualTo(300);
        assertThat(reloaded.getPerceivedEffort()).isEqualTo((short) 4);
        assertThat(reloaded.getNotes()).isEqualTo("Comfortable morning run");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();

    }

    @Test 
    void findsRunForItsOwner() {
        // Arrange: valid data with predictable values.
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-17T10:00:00Z");

        Run run = new Run(
                userId,
                startedAt,
                RunType.EASY,
                new BigDecimal("5.000"),
                1500,
                300,
                (short) 4,
                "Comfortable morning run");

        // Act: write to PostgreSQL, then force a fresh read.
        Run saved = runRepository.saveAndFlush(run);
        UUID runId = saved.getId();

        entityManager.clear();

        Optional<Run> result = runRepository.findByIdAndUserId(runId, userId);

        assertThat(result).isPresent();
        Run found = result.orElseThrow();

        // Assert: verify values loaded from PostgreSQL.
        assertThat(found.getId()).isEqualTo(runId);
        assertThat(found.getUserId()).isEqualTo(userId);
    }

    @Test
    void doesNotFindRunForDifferentOwner() {

        UUID ownerId = UUID.randomUUID();
        UUID differentOwnerId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-17T10:00:00Z");

        Run run = new Run(
                ownerId,
                startedAt,
                RunType.EASY,
                new BigDecimal("5.000"),
                1500,
                300,
                (short) 4,
                "Comfortable morning run");

        Run saved = runRepository.saveAndFlush(run);
        UUID runId = saved.getId();

        entityManager.clear();

        Optional<Run> result = runRepository.findByIdAndUserId(runId, differentOwnerId);

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.000", "-1.000"})
    void rejectsNonPositiveDistance(String distance) {
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-17T10:00:00Z");

        Run run = new Run(
                userId,
                startedAt,
                RunType.EASY,
                new BigDecimal(distance),
                1500,
                300,
                (short) 4,
                "Comfortable morning run");

        assertThatThrownBy(() -> runRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("chk_runs_distance_positive");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveDuration(int duration) {
        UUID userId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-17T10:00:00Z");

        Run run = new Run(
                userId,
                startedAt,
                RunType.EASY,
                new BigDecimal("5.000"),
                duration,
                300,
                (short) 4,
                "Comfortable morning run");

        assertThatThrownBy(() -> runRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("chk_runs_duration_positive");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePace(int paceSeconds) {
        Run run = runForTest(
                RunType.EASY, paceSeconds, (short) 4, "Morning run"
        );

        assertThatThrownBy(() -> runRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("chk_runs_average_pace_positive");
        }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void rejectsOutOfRangePerceivedEffort(int effort) {
        Run run = runForTest(
                RunType.EASY, 300, (short) effort, "Morning run"
        );

        assertThatThrownBy(() -> runRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("chk_runs_perceived_effort_range");
        }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void acceptsPerceivedEffortBoundaries(int effort) {
        Run saved = runRepository.saveAndFlush(
                runForTest(RunType.EASY, 300, (short) effort, "Morning run")
        );
        UUID runId = saved.getId();
        entityManager.clear();

        Run reloaded = runRepository.findById(runId).orElseThrow();

        assertThat(reloaded.getPerceivedEffort()).isEqualTo((short) effort);
        }

    @Test
    void persistsRunWithoutOptionalFields() {
        Run saved = runRepository.saveAndFlush(
                runForTest(RunType.EASY, 300, null, null)
        );
        UUID runId = saved.getId();
        entityManager.clear();

        Run reloaded = runRepository.findById(runId).orElseThrow();

        assertThat(reloaded.getPerceivedEffort()).isNull();
        assertThat(reloaded.getNotes()).isNull();
        }

    @ParameterizedTest
    @EnumSource(RunType.class)
    void persistsEverySupportedRunType(RunType type) {
        Run saved = runRepository.saveAndFlush(
                runForTest(type, 300, (short) 4, "Morning run")
        );
        UUID runId = saved.getId();
        entityManager.clear();

        Run reloaded = runRepository.findById(runId).orElseThrow();

        assertThat(reloaded.getRunType()).isEqualTo(type);
        }

    @Test
    void acceptsNotesAtMaximumLength() {
        String notes = "a".repeat(1000);
        Run saved = runRepository.saveAndFlush(
                runForTest(RunType.EASY, 300, (short) 4, notes)
        );
        UUID runId = saved.getId();
        entityManager.clear();

        Run reloaded = runRepository.findById(runId).orElseThrow();

        assertThat(reloaded.getNotes()).isEqualTo(notes);
        }

    @Test
    void rejectsNotesLongerThanMaximumLength() {
        Run run = runForTest(
                RunType.EASY, 300, (short) 4, "a".repeat(1001)
        );

        assertThatThrownBy(() -> runRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("character varying(1000)");
        }
}