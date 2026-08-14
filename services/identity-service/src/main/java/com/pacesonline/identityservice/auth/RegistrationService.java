package com.pacesonline.identityservice.auth;

import com.pacesonline.identityservice.user.User;
import com.pacesonline.identityservice.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uq_users_email";

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(password);

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordHash
        );

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyRegisteredException();
            }

            throw exception;
        }
    }

    private boolean isEmailUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && EMAIL_UNIQUE_CONSTRAINT.equals(
                            constraintViolation.getConstraintName()
                    )) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}