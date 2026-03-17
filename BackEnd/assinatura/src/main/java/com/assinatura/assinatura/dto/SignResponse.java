package com.assinatura.assinatura.dto;

import java.time.LocalDateTime;

public record SignResponse(
        Long id,
        String publicId,
        String signatureBase64,
        String hashAlgorithm,
        String signatureAlgorithm,
        LocalDateTime createdAt
) {
}
