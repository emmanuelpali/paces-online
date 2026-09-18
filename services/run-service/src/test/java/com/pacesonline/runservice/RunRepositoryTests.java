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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
}