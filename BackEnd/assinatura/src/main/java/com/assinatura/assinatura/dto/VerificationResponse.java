package com.assinatura.assinatura.dto;

import java.time.LocalDateTime;

public record VerificationResponse(
        boolean valid,
        String publicId,
        String signerName,
        String hashAlgorithm,
        String signatureAlgorithm,
        LocalDateTime createdAt,
        String message
) {
}
