package com.pacesonline.identityservice.profile;

import com.pacesonline.identityservice.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfiguration.class)
class UserProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserProfileService userProfileService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void getUserWithoutAccessTokenAndReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/user"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }

    @Test
    void getUserWithAuthenticatedJwtReturnsProfile() throws Exception {
        UUID userId =
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        Instant createdAt =
                Instant.parse("2026-08-18T15:00:00Z");

        UserProfileResponse response =
                new UserProfileResponse(
                        userId,
                        "runner@example.com",
                        createdAt
                );

        when(userProfileService.getUserProfile(userId))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/users/user")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject(userId.toString())
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("runner@example.com"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-08-18T15:00:00Z"));

        verify(userProfileService)
                .getUserProfile(userId);
    }
}