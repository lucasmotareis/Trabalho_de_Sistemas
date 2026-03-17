package com.assinatura.assinatura.service;

import com.assinatura.assinatura.domain.entity.Signature;
import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.domain.entity.UserKey;
import com.assinatura.assinatura.dto.SignResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.exception.MissingUserKeyException;
import com.assinatura.assinatura.repository.SignatureRepository;
import com.assinatura.assinatura.repository.UserKeyRepository;
import com.assinatura.assinatura.repository.UserRepository;
import com.assinatura.assinatura.service.crypto.PrivateKeyPreparationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class SignService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final SignatureRepository signatureRepository;
    private final PrivateKeyPreparationService privateKeyPreparationService;

    public SignService(UserRepository userRepository,
                       UserKeyRepository userKeyRepository,
                       SignatureRepository signatureRepository,
                       PrivateKeyPreparationService privateKeyPreparationService) {
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.signatureRepository = signatureRepository;
        this.privateKeyPreparationService = privateKeyPreparationService;
    }

    @Transactional
    public SignResponse sign(String authenticatedEmail, String originalText) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        UserKey userKey = userKeyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new MissingUserKeyException(user.getId()));

        PrivateKey privateKey = privateKeyPreparationService.reconstructPrivateKey(userKey.getPrivateKeyEncrypted());
        String signatureBase64 = signText(originalText, privateKey);
        String textHash = hashText(originalText);

        Signature signature = new Signature();
        signature.setUser(user);
        signature.setOriginalText(originalText);
        signature.setTextHash(textHash);
        signature.setSignatureBase64(signatureBase64);
        signature.setHashAlgorithm(HASH_ALGORITHM);
        signature.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
        signature.setCreatedAt(LocalDateTime.now());
        signature.setPublicId(UUID.randomUUID().toString());

        Signature savedSignature = signatureRepository.save(signature);

        return new SignResponse(
                savedSignature.getId(),
                savedSignature.getPublicId(),
                savedSignature.getSignatureBase64(),
                savedSignature.getHashAlgorithm(),
                savedSignature.getSignatureAlgorithm(),
                savedSignature.getCreatedAt()
        );
    }

    private String signText(String text, PrivateKey privateKey) {
        try {
            java.security.Signature signer = java.security.Signature.getInstance(SIGNATURE_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign text", exception);
        }
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash text", exception);
        }
    }
}
