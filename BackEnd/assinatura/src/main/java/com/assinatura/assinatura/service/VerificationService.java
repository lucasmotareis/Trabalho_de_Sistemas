package com.assinatura.assinatura.service;

import com.assinatura.assinatura.domain.entity.Signature;
import com.assinatura.assinatura.domain.entity.UserKey;
import com.assinatura.assinatura.domain.entity.VerificationLog;
import com.assinatura.assinatura.dto.VerifyRequest;
import com.assinatura.assinatura.dto.VerificationResponse;
import com.assinatura.assinatura.exception.MissingUserKeyException;
import com.assinatura.assinatura.exception.SignatureNotFoundException;
import com.assinatura.assinatura.repository.SignatureRepository;
import com.assinatura.assinatura.repository.UserKeyRepository;
import com.assinatura.assinatura.repository.VerificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class VerificationService {

    private final SignatureRepository signatureRepository;
    private final UserKeyRepository userKeyRepository;
    private final VerificationLogRepository verificationLogRepository;

    public VerificationService(SignatureRepository signatureRepository,
                               UserKeyRepository userKeyRepository,
                               VerificationLogRepository verificationLogRepository) {
        this.signatureRepository = signatureRepository;
        this.userKeyRepository = userKeyRepository;
        this.verificationLogRepository = verificationLogRepository;
    }

    @Transactional
    public VerificationResponse verifyByPublicId(String publicId) {
        Signature signature = signatureRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SignatureNotFoundException(publicId));
        UserKey userKey = userKeyRepository.findByUserId(signature.getUser().getId())
                .orElseThrow(() -> new MissingUserKeyException(signature.getUser().getId()));

        boolean valid = verifySignature(signature.getOriginalText(), signature.getSignatureBase64(), signature.getSignatureAlgorithm(), userKey.getPublicKey());
        String message = valid ? "Signature is valid" : "Signature is invalid";

        VerificationLog verificationLog = new VerificationLog();
        verificationLog.setSignature(signature);
        verificationLog.setProvidedText(signature.getOriginalText());
        verificationLog.setProvidedSignatureBase64(signature.getSignatureBase64());
        verificationLog.setValid(valid);
        verificationLog.setMessage(message);
        verificationLog.setVerifiedAt(LocalDateTime.now());
        verificationLogRepository.save(verificationLog);

        return new VerificationResponse(
                valid,
                signature.getPublicId(),
                signature.getUser().getNome(),
                signature.getHashAlgorithm(),
                signature.getSignatureAlgorithm(),
                signature.getCreatedAt(),
                message
        );
    }

    @Transactional
    public VerificationResponse verifyManual(VerifyRequest request) {
        Signature signature = signatureRepository.findByPublicId(request.publicId())
                .orElseThrow(() -> new SignatureNotFoundException(request.publicId()));
        UserKey userKey = userKeyRepository.findByUserId(signature.getUser().getId())
                .orElseThrow(() -> new MissingUserKeyException(signature.getUser().getId()));

        boolean valid = verifySignature(request.text(), request.signatureBase64(), signature.getSignatureAlgorithm(), userKey.getPublicKey());
        String message = valid ? "Signature is valid" : "Signature is invalid";

        VerificationLog verificationLog = new VerificationLog();
        verificationLog.setSignature(signature);
        verificationLog.setProvidedText(request.text());
        verificationLog.setProvidedSignatureBase64(request.signatureBase64());
        verificationLog.setValid(valid);
        verificationLog.setMessage(message);
        verificationLog.setVerifiedAt(LocalDateTime.now());
        verificationLogRepository.save(verificationLog);

        return new VerificationResponse(
                valid,
                signature.getPublicId(),
                signature.getUser().getNome(),
                signature.getHashAlgorithm(),
                signature.getSignatureAlgorithm(),
                signature.getCreatedAt(),
                message
        );
    }

    private boolean verifySignature(String text, String signatureBase64, String signatureAlgorithm, String publicKeyBase64) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));

            java.security.Signature verifier = java.security.Signature.getInstance(signatureAlgorithm);
            verifier.initVerify(publicKey);
            verifier.update(text.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }
}
