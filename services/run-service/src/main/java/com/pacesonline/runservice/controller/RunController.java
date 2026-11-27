package com.pacesonline.runservice.controller;

import java.net.URI;
import java.util.UUID;

import com.pacesonline.runservice.dto.CreateRunRequest;
import com.pacesonline.runservice.dto.RunResponse;
import com.pacesonline.runservice.service.RunService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    public ResponseEntity<RunResponse> createRun(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRunRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        RunResponse createdRun = runService.createRun(userId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{runId}")
                .buildAndExpand(createdRun.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdRun);
    }
}
