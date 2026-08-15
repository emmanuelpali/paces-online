package com.pacesonline.identityservice.auth.login;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private static final String TOKEN_TYPE = "Bearer";

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = loginService.login(
                request.email(),
                request.password()
        );

        return new LoginResponse(
                result.accessToken(),
                TOKEN_TYPE,
                result.expiresIn()
        );
    }
}