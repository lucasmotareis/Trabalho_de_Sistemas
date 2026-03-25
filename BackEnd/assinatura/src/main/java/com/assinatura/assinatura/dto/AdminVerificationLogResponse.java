package com.assinatura.assinatura.dto;

import java.time.LocalDateTime;

public record AdminVerificationLogResponse(
        Long logId,
        String signaturePublicId,
        Long verifiedByUserId,
        String verifiedByUserEmail,
        boolean valid,
        String message,
        LocalDateTime verifiedAt
) {
}
