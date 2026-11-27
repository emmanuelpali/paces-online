package com.pacesonline.runservice;

import static org.assertj.core.api.Assertions.assertThat;
import com.pacesonline.runservice.exception.InvalidRunPaceException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.pacesonline.runservice.exception.RunNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

import com.pacesonline.runservice.dto.CreateRunRequest;
import com.pacesonline.runservice.entity.Run;
import com.pacesonline.runservice.entity.RunType;
import com.pacesonline.runservice.mapper.RunMapper;
import com.pacesonline.runservice.repository.RunRepository;
import com.pacesonline.runservice.service.RunService;

@ExtendWith(MockitoExtension.class)
public class RunServiceTests {

    @Mock
    private RunRepository runRepository;

    private RunService runService;
    
    @BeforeEach
    void setUp() {
        runService = new RunService(runRepository, new RunMapper());
    }

    @Test
    void createsRunForAuthenticatedUserAndCalculatesRoundedPace() {
        UUID userId = UUID.randomUUID();
        Integer durationSeconds = 1001;
        BigDecimal distanceKilometres = new BigDecimal("2.000");
        Instant startedAt = Instant.parse("2026-11-12T18:00:00Z");
        RunType runType = RunType.EASY;
        Short perceivedEffort = 5;

        CreateRunRequest request = new CreateRunRequest(
                startedAt,
                runType,
                distanceKilometres,
                durationSeconds,
                perceivedEffort,
                null);

        when(runRepository.save(any(Run.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runService.createRun(userId, request);

        ArgumentCaptor<Run> runCaptor = ArgumentCaptor.forClass(Run.class);

        verify(runRepository).save(runCaptor.capture());

        Run savedRun = runCaptor.getValue();

        assertThat(savedRun.getUserId()).isEqualTo(userId);
        assertThat(savedRun.getAveragePaceSecondsPerKilometre()).isEqualTo(501);
        assertThat(savedRun.getStartedAt()).isEqualTo(startedAt);
        assertThat(savedRun.getRunType()).isEqualTo(runType);
        assertThat(savedRun.getDistanceKilometres()).isEqualTo(distanceKilometres);
        assertThat(savedRun.getDurationSeconds()).isEqualTo(durationSeconds);
        assertThat(savedRun.getPerceivedEffort()).isEqualTo(perceivedEffort);
        assertThat(savedRun.getNotes()).isNull();    
    }

    @ParameterizedTest
    @CsvSource({
            "3.000, 1000, 333",
            "4.000, 1200, 300"
    })
    void calculatesPaceInWholeSeconds(
            String distance,
            int duration,
            int expectedPace) {

        CreateRunRequest request = new CreateRunRequest(
                Instant.parse("2026-11-12T18:00:00Z"),
                RunType.EASY,
                new BigDecimal(distance),
                duration,
                null,
                null);

        when(runRepository.save(any(Run.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runService.createRun(UUID.randomUUID(), request);

        ArgumentCaptor<Run> runCaptor =
                ArgumentCaptor.forClass(Run.class);

        verify(runRepository).save(runCaptor.capture());

        assertThat(runCaptor.getValue().getAveragePaceSecondsPerKilometre())
                .isEqualTo(expectedPace);
    }

    @ParameterizedTest
    @CsvSource({
            "3.000, 1",
            "0.001, 2147484"
       }) 
        void rejectsUnstorablePaceBeforeSaving(String distance, int durationSeconds) {
        UUID userId = UUID.randomUUID();
        CreateRunRequest request = new CreateRunRequest(
                Instant.parse("2026-11-12T18:00:00Z"),
                RunType.EASY,
                new BigDecimal(distance),
                durationSeconds,
                null,
                null);

        assertThatThrownBy(() -> runService.createRun(userId, request))
                .isInstanceOf(InvalidRunPaceException.class);

        verifyNoInteractions(runRepository);
     }

    @Test
    void returnsRunFoundByIdAndOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        Run run = new Run(ownerId, Instant.parse("2026-11-12T18:00:00Z"),
                RunType.EASY, new BigDecimal("2.000"), 1001, 501, null, null);
        when(runRepository.findByIdAndUserId(runId, ownerId)).thenReturn(Optional.of(run));

        var response = runService.getRun(ownerId, runId);

        assertThat(response.runType()).isEqualTo(RunType.EASY);
        assertThat(response.averagePaceSecondsPerKilometre()).isEqualTo(501);
    }

    @Test
    void reportsNotFoundWhenMissingOrNotOwned() {
        UUID ownerId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        when(runRepository.findByIdAndUserId(runId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runService.getRun(ownerId, runId))
                .isInstanceOf(RunNotFoundException.class);

        verify(runRepository).findByIdAndUserId(runId, ownerId);
        verifyNoMoreInteractions(runRepository);
    }
}
