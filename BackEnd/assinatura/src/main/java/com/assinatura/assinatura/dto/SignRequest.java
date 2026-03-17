package com.assinatura.assinatura.dto;

import jakarta.validation.constraints.NotBlank;

public record SignRequest(
        @NotBlank(message = "text is required")
        String text
) {
}
