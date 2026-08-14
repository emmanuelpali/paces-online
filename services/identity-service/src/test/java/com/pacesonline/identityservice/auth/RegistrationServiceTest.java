package com.pacesonline.identityservice.auth;

import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.hibernate.exception.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registersUserWithNormalizedEmailAndHashedPassword() {
        RegistrationService registrationService =
                new RegistrationService(userRepository, passwordEncoder);

        when(userRepository.existsByEmail("runner@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("strong-password"))
                .thenReturn("hashed-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = registrationService.register(
                " Runner@Example.com ",
                "strong-password"
        );

        assertThat(user.getEmail()).isEqualTo("runner@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getId()).isNotNull();

        verify(passwordEncoder).encode("strong-password");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsAlreadyRegisteredEmail() {
        RegistrationService registrationService =
                new RegistrationService(userRepository, passwordEncoder);

        when(userRepository.existsByEmail("runner@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                registrationService.register(
                        "Runner@Example.com",
                        "strong-password"
                )
        )
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email is already registered");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void checksDuplicateUsingNormalizedEmail() {
        RegistrationService registrationService =
                new RegistrationService(userRepository, passwordEncoder);

        when(userRepository.existsByEmail("runner@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                registrationService.register(
                        " RUNNER@EXAMPLE.COM ",
                        "strong-password"
                )
        ).isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository).existsByEmail("runner@example.com");
    }

    @Test
        void rejectsEmailWhenDatabaseUniqueConstraintIsViolated() {
        RegistrationService registrationService =
                new RegistrationService(userRepository, passwordEncoder);

        when(userRepository.existsByEmail("runner@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("strong-password"))
                .thenReturn("hashed-password");

        ConstraintViolationException constraintViolation =
                mock(ConstraintViolationException.class);

        when(constraintViolation.getConstraintName())
                .thenReturn("uq_users_email");

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Unique constraint violated",
                        constraintViolation
                );

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(databaseException);

        assertThatThrownBy(() ->
                registrationService.register(
                        "runner@example.com",
                        "strong-password"
                )
        ).isInstanceOf(EmailAlreadyRegisteredException.class);
        }
}