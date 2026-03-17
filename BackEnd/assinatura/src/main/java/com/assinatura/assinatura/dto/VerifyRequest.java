package com.assinatura.assinatura.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(
        @NotBlank(message = "publicId is required")
        String publicId,

        @NotBlank(message = "text is required")
        String text,

        @NotBlank(message = "signatureBase64 is required")
        String signatureBase64
) {
}
