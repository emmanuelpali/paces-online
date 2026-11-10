package com.pacesonline.runservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.pacesonline.runservice.dto.CreateRunRequest;
import com.pacesonline.runservice.dto.RunResponse;
import com.pacesonline.runservice.entity.Run;
import com.pacesonline.runservice.mapper.RunMapper;
import com.pacesonline.runservice.repository.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunService {

    private final RunRepository runRepository;
    private final RunMapper runMapper;

    public RunService(
            RunRepository runRepository,
            RunMapper runMapper
    ) {
        this.runRepository = runRepository;
        this.runMapper = runMapper;
    }

    @Transactional
    public RunResponse createRun(
            UUID userId,
            CreateRunRequest request
    ) {
        int averagePaceSecondsPerKilometre = calculateAveragePace(
                request.durationSeconds(),
                request.distanceKilometres()
        );

        Run run = runMapper.toEntity(
                userId,
                request,
                averagePaceSecondsPerKilometre
        );

        Run savedRun = runRepository.save(run);

        return runMapper.toResponse(savedRun);
    }

    private int calculateAveragePace(
            int durationSeconds,
            BigDecimal distanceKilometres
    ) {
        return BigDecimal.valueOf(durationSeconds)
                .divide(distanceKilometres, 0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
