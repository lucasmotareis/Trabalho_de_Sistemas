package com.assinatura.assinatura.exception;

public class MissingUserKeyException extends RuntimeException {

    public MissingUserKeyException(Long userId) {
        super("No key pair found for user id: " + userId);
    }
}
