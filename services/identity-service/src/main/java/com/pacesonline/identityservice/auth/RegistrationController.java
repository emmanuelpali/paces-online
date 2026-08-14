package com.pacesonline.identityservice.auth;

import com.pacesonline.identityservice.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisteredUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        User user = registrationService.register(
                request.email(),
                request.password()
        );

        return new RegisteredUserResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}