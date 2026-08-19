package com.pacesonline.identityservice.auth.registration;

import com.pacesonline.identityservice.auth.registration.EmailAlreadyRegisteredException;
import com.pacesonline.identityservice.auth.registration.RegistrationController;
import com.pacesonline.identityservice.auth.registration.RegistrationService;
import com.pacesonline.identityservice.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.pacesonline.identityservice.config.SecurityConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfiguration.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void registersUser() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-13T20:00:00Z");

        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn("runner@example.com");
        when(user.getCreatedAt()).thenReturn(createdAt);

        when(registrationService.register(
                "runner@example.com",
                "strong-password"
        )).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "runner@example.com",
                                  "password": "strong-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("runner@example.com"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-08-13T20:00:00Z"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "strong-password"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(registrationService, never())
                .register(anyString(), anyString());
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "runner@example.com",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(registrationService, never())
                .register(anyString(), anyString());
    }

    @Test
    void returnsConflictWhenEmailAlreadyExists() throws Exception {
        when(registrationService.register(
                "runner@example.com",
                "strong-password"
        )).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "runner@example.com",
                                  "password": "strong-password"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}