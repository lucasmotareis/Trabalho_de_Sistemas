package com.assinatura.assinatura.service.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AesGcmKeyEncryptionService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec secretKey;

    public AesGcmKeyEncryptionService(@Value("${app.crypto.master-key}") String masterKeyBase64) {
        this.secretKey = new SecretKeySpec(parseAndValidateMasterKey(masterKeyBase64), "AES");
    }

    public String encryptPrivateKey(byte[] privateKeyBytes) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(privateKeyBytes);

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt private key", exception);
        }
    }

    public byte[] decryptPrivateKey(String encryptedPayloadBase64) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedPayloadBase64);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted payload is too short");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Failed to decrypt private key payload", exception);
        }
    }

    private static byte[] parseAndValidateMasterKey(String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalArgumentException("app.crypto.master-key must be provided");
        }

        byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
        int keyLength = decodedKey.length;
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException("Invalid AES master key length: " + keyLength);
        }
        return decodedKey;
    }
}
