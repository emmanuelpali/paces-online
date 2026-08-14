package com.pacesonline.identityservice.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password

) {
}