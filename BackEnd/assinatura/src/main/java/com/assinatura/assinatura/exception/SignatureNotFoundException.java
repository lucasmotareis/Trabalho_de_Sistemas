package com.assinatura.assinatura.exception;

public class SignatureNotFoundException extends RuntimeException {

    public SignatureNotFoundException(String publicId) {
        super("Signature not found for publicId: " + publicId);
    }
}
