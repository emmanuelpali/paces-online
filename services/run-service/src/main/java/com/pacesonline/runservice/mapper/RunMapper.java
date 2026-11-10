package com.pacesonline.runservice.mapper;

import java.util.UUID;

import com.pacesonline.runservice.dto.CreateRunRequest;
import com.pacesonline.runservice.dto.RunResponse;
import com.pacesonline.runservice.entity.Run;
import org.springframework.stereotype.Component;

@Component
public class RunMapper {

    public Run toEntity(
            UUID userId,
            CreateRunRequest request,
            int averagePaceSecondsPerKilometre
    ) {
        return new Run(
                userId,
                request.startedAt(),
                request.runType(),
                request.distanceKilometres(),
                request.durationSeconds(),
                averagePaceSecondsPerKilometre,
                request.perceivedEffort(),
                request.notes()
        );
    }

    public RunResponse toResponse(Run run) {
        return new RunResponse(
                run.getId(),
                run.getUserId(),
                run.getStartedAt(),
                run.getRunType(),
                run.getDistanceKilometres(),
                run.getDurationSeconds(),
                run.getAveragePaceSecondsPerKilometre(),
                run.getPerceivedEffort(),
                run.getNotes(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }
}
