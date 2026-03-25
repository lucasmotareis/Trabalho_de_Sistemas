package com.assinatura.assinatura.service;

import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.domain.entity.UserKey;
import com.assinatura.assinatura.domain.entity.UserRole;
import com.assinatura.assinatura.dto.SignupRequest;
import com.assinatura.assinatura.dto.SignupResponse;
import com.assinatura.assinatura.exception.DuplicateEmailException;
import com.assinatura.assinatura.repository.UserKeyRepository;
import com.assinatura.assinatura.repository.UserRepository;
import com.assinatura.assinatura.service.crypto.AesGcmKeyEncryptionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class SignupService {

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesGcmKeyEncryptionService keyEncryptionService;

    public SignupService(UserRepository userRepository,
                         UserKeyRepository userKeyRepository,
                         PasswordEncoder passwordEncoder,
                         AesGcmKeyEncryptionService keyEncryptionService) {
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.keyEncryptionService = keyEncryptionService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setNome(request.nome().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.ROLE_USER);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        KeyPair keyPair = generateKeyPair();
        UserKey userKey = new UserKey();
        userKey.setUser(savedUser);
        userKey.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        userKey.setPrivateKeyEncrypted(keyEncryptionService.encryptPrivateKey(keyPair.getPrivate().getEncoded()));
        userKey.setAlgorithm(KEY_ALGORITHM);
        userKey.setKeySize(KEY_SIZE);
        userKey.setCreatedAt(now);
        userKeyRepository.save(userKey);

        return new SignupResponse(savedUser.getId(), savedUser.getNome(), savedUser.getEmail());
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            return keyPairGenerator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not generate key pair", exception);
        }
    }
}
