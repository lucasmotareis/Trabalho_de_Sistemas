package com.assinatura.assinatura.dto;

public record PublicUserKeyResponse(
        Long userId,
        String email,
        String publicKey
) {
}
