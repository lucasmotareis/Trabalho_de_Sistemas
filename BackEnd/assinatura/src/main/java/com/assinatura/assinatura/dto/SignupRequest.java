package com.assinatura.assinatura.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "nome is required")
        @Size(max = 150, message = "nome must have at most 150 characters")
        String nome,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must have at most 255 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 255, message = "password must have between 8 and 255 characters")
        String password
) {
}
