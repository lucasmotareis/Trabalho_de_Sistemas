package com.assinatura.assinatura.dto;

import java.time.LocalDateTime;

public record MyVerificationLogResponse(
        Long logId,
        String signaturePublicId,
        boolean valid,
        String message,
        LocalDateTime verifiedAt
) {
}
