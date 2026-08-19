package com.pacesonline.identityservice.profile;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;

import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserProfileService userProfileService;

    @Test
    void returnsProfileForUser() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("runner@example.com");
        when(user.getCreatedAt()).thenReturn(createdAt);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));


        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserProfileResponse result =
                userProfileService.getUserProfile(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.email())
                .isEqualTo("runner@example.com");
        assertThat(result.createdAt())
                .isEqualTo(createdAt);
        verify(userRepository).findById(userId);
    }
}