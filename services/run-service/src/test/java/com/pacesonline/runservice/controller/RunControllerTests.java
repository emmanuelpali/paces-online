package com.pacesonline.runservice.controller;

import com.pacesonline.runservice.config.SecurityConfiguration;
import com.pacesonline.runservice.dto.CreateRunRequest;
import com.pacesonline.runservice.dto.RunResponse;
import com.pacesonline.runservice.entity.RunType;
import com.pacesonline.runservice.exception.InvalidRunPaceException;
import com.pacesonline.runservice.exception.RunNotFoundException;
import com.pacesonline.runservice.service.RunService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
@Import(SecurityConfiguration.class)
class RunControllerTests {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private RunService runService;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        private static final String VALID_RUN_JSON = """
                        {
                          "startedAt": "2026-11-12T18:00:00Z",
                          "runType": "EASY",
                          "distanceKilometres": 2.000,
                          "durationSeconds": 1001
                        }
                        """;

        @Test
        void createsRunForAuthenticatedUser() throws Exception {
                UUID runId = UUID.fromString("22222222-2222-2222-2222-222222222222");
                UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
                Instant startedAt = Instant.parse("2026-11-12T18:00:00Z");
                Instant savedAt = Instant.parse("2026-11-12T18:20:00Z");

                RunResponse response = new RunResponse(
                                runId,
                                startedAt,
                                RunType.EASY,
                                new BigDecimal("2.000"),
                                1001,
                                501,
                                (short) 5,
                                "Evening run",
                                savedAt,
                                savedAt);

                when(runService.createRun(eq(userId), any(CreateRunRequest.class))).thenReturn(response);

                mockMvc.perform(
                                post("/api/v1/runs")
                                                .with(jwt().jwt(token -> token.subject(userId.toString())))
                                                .contentType("application/json")
                                                .content("""
                                                                {
                                                                "startedAt": "2026-11-12T18:00:00Z",
                                                                "runType": "EASY",
                                                                "distanceKilometres": 2.000,
                                                                "durationSeconds": 1001,
                                                                "perceivedEffort": 5,
                                                                "notes": "Evening run"
                                                                }
                                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "http://localhost/api/v1/runs/" + runId))
                                .andExpect(jsonPath("$.id").value(runId.toString()))
                                .andExpect(jsonPath("$.averagePaceSecondsPerKilometre").value(501))
                                .andExpect(jsonPath("$.userId").doesNotExist());

                verify(runService).createRun(eq(userId), any(CreateRunRequest.class));
        }

        @Test
        void rejectsRunCreationWithoutAuthentication() throws Exception {
                mockMvc.perform(post("/api/v1/runs")
                                .contentType("application/json")
                                .content(VALID_RUN_JSON))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(runService);
        }

        @Test
        void rejectsRunCreationWithZeroDuration() throws Exception {
                String invalidJson = VALID_RUN_JSON.replace(
                                "\"durationSeconds\": 1001", "\"durationSeconds\": 0");

                mockMvc.perform(post("/api/v1/runs")
                                .with(jwt().jwt(token -> token.subject("11111111-1111-1111-1111-111111111111")))
                                .contentType("application/json")
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(runService);
        }

        @Test
        void returnsBadRequestWhenServiceRejectsCalculatedPace() throws Exception {
             UUID userId = UUID.randomUUID();
             when(runService.createRun(eq(userId), any(CreateRunRequest.class)))
                            .thenThrow(new InvalidRunPaceException());
             mockMvc.perform(post("/api/v1/runs")
                            .with(jwt().jwt(token -> token.subject(userId.toString())))
                            .contentType("application/json")
                            .content("""
                                        {
                                         "startedAt": "2026-11-12T18:00:00Z",
                                         "runType": "EASY",
                                         "distanceKilometres": 3.000,
                                         "durationSeconds": 1
                                        }
                                        """))                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));

                verify(runService).createRun(eq(userId), any(CreateRunRequest.class));
        }

        @Test
        void retrievesOwnedRun() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID runId = UUID.randomUUID();
                Instant timestamp = Instant.parse("2026-11-12T18:00:00Z");
                when(runService.getRun(ownerId, runId)).thenReturn(new RunResponse(
                                runId, timestamp, RunType.EASY, new BigDecimal("2.000"),
                                1001, 501, null, null, timestamp, timestamp));

                mockMvc.perform(get("/api/v1/runs/{runId}", runId)
                                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(runId.toString()))
                                .andExpect(jsonPath("$.userId").doesNotExist());

                verify(runService).getRun(ownerId, runId);
        }

        @Test
        void rejectsRunRetrievalWithoutAuthentication() throws Exception {
                mockMvc.perform(get("/api/v1/runs/{runId}", UUID.randomUUID()))
                                .andExpect(status().isUnauthorized());
                verifyNoInteractions(runService);
        }

        @Test
        void rejectsInvalidRunId() throws Exception {
                mockMvc.perform(get("/api/v1/runs/{runId}", "not-a-uuid")
                                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(runService);
        }

        @Test
        void returnsNotFoundWhenRunIsNotVisible() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID runId = UUID.randomUUID();
                when(runService.getRun(ownerId, runId)).thenThrow(
                                new RunNotFoundException());

                mockMvc.perform(get("/api/v1/runs/{runId}", runId)
                                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.detail").value("Run not found."));
        }
}
