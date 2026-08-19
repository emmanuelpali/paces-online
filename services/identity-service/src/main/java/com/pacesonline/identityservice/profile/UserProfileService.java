package com.pacesonline.identityservice.profile;

import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}