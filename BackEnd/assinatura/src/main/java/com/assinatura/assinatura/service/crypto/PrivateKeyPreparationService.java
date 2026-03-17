package com.assinatura.assinatura.service.crypto;

import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

@Service
public class PrivateKeyPreparationService {

    private final AesGcmKeyEncryptionService keyEncryptionService;

    public PrivateKeyPreparationService(AesGcmKeyEncryptionService keyEncryptionService) {
        this.keyEncryptionService = keyEncryptionService;
    }

    public PrivateKey reconstructPrivateKey(String privateKeyEncrypted) {
        try {
            byte[] privateKeyBytes = keyEncryptionService.decryptPrivateKey(privateKeyEncrypted);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to reconstruct private key", exception);
        }
    }
}
