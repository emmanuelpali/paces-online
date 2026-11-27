package com.pacesonline.runservice;

import com.jayway.jsonpath.JsonPath;
import com.pacesonline.runservice.entity.Run;
import com.pacesonline.runservice.entity.RunType;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.pacesonline.runservice.repository.RunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, SecurityTestConfiguration.class})
class RunCreationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RunRepository runRepository;

    @Test
    void persistsJwtOwnerAndCalculatedPaceDespiteClientSuppliedValues() throws Exception {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String accessToken = createAccessToken(ownerId);

        String responseBody = mockMvc.perform(post("/api/v1/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAt": "2026-11-12T18:00:00Z",
                                  "runType": "EASY",
                                  "distanceKilometres": 2.000,
                                  "durationSeconds": 1001,
                                  "userId": "22222222-2222-2222-2222-222222222222",
                                  "averagePaceSecondsPerKilometre": 999
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String responseId = JsonPath.read(responseBody, "$.id");
        UUID runId = UUID.fromString(responseId);

        Run persistedRun = runRepository.findByIdAndUserId(runId, ownerId)
                .orElseThrow(() -> new AssertionError(
                        "Expected run " + runId + " to be persisted for owner " + ownerId));

        assertThat(persistedRun.getAveragePaceSecondsPerKilometre()).isEqualTo(501);
    }

    @Test
    void onlyOwnerCanRetrieveRunAndOtherCallersSeeNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Run run = runRepository.saveAndFlush(new Run(
                ownerId, Instant.parse("2026-11-12T18:00:00Z"), RunType.EASY,
                new BigDecimal("2.000"), 1001, 501, null, null));

        mockMvc.perform(get("/api/v1/runs/{runId}", run.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(run.getId().toString()));

        String otherBearer = "Bearer " + createAccessToken(UUID.randomUUID());
        for (UUID runId : new UUID[] {run.getId(), UUID.randomUUID()}) {
            mockMvc.perform(get("/api/v1/runs/{runId}", runId)
                            .header(HttpHeaders.AUTHORIZATION, otherBearer))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Run not found."));
        }
    }

    private String createAccessToken(UUID ownerId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityTestConfiguration.TEST_ISSUER)
                .subject(ownerId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
