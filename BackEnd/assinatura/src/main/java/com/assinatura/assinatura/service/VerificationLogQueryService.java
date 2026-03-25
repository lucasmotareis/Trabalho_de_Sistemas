package com.assinatura.assinatura.service;

import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.dto.AdminVerificationLogResponse;
import com.assinatura.assinatura.dto.MyVerificationLogResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.repository.UserRepository;
import com.assinatura.assinatura.repository.VerificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class VerificationLogQueryService {

    private final VerificationLogRepository verificationLogRepository;
    private final UserRepository userRepository;

    public VerificationLogQueryService(VerificationLogRepository verificationLogRepository,
                                       UserRepository userRepository) {
        this.verificationLogRepository = verificationLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MyVerificationLogResponse> listMyVerificationLogs(String authenticatedEmail) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        return verificationLogRepository.findAllByVerifiedByUserId(user.getId()).stream()
                .map(log -> new MyVerificationLogResponse(
                        log.getId(),
                        log.getSignature().getPublicId(),
                        log.isValid(),
                        log.getMessage(),
                        log.getVerifiedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminVerificationLogResponse> listAllVerificationLogs() {
        return verificationLogRepository.findAllWithSignatureAndVerifier().stream()
                .map(log -> new AdminVerificationLogResponse(
                        log.getId(),
                        log.getSignature().getPublicId(),
                        log.getVerifiedByUser() == null ? null : log.getVerifiedByUser().getId(),
                        log.getVerifiedByUser() == null ? null : log.getVerifiedByUser().getEmail(),
                        log.isValid(),
                        log.getMessage(),
                        log.getVerifiedAt()
                ))
                .toList();
    }
}
