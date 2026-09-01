package com.pacesonline.identityservice.auth.refreshtoken;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RefreshTokenController {

    private static final String TOKEN_TYPE = "Bearer";

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenController(
            RefreshTokenService refreshTokenService
    ) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshTokenResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        RefreshTokenRotationResult result =
                refreshTokenService.rotate(request.refreshToken());

        return new RefreshTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                TOKEN_TYPE,
                result.accessTokenExpiresIn(),
                result.refreshTokenExpiresIn()
        );
    }
}