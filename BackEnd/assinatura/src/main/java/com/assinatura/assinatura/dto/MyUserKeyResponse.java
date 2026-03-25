package com.assinatura.assinatura.dto;

public record MyUserKeyResponse(
        Long userId,
        String email,
        String publicKey,
        String privateKey
) {
}
