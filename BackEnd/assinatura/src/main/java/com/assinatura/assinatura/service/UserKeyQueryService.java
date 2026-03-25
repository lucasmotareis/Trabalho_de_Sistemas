package com.assinatura.assinatura.service;

import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.domain.entity.UserKey;
import com.assinatura.assinatura.dto.MyUserKeyResponse;
import com.assinatura.assinatura.dto.PublicUserKeyResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.exception.MissingUserKeyException;
import com.assinatura.assinatura.repository.UserKeyRepository;
import com.assinatura.assinatura.repository.UserRepository;
import com.assinatura.assinatura.service.crypto.AesGcmKeyEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class UserKeyQueryService {

    private final UserKeyRepository userKeyRepository;
    private final UserRepository userRepository;
    private final AesGcmKeyEncryptionService keyEncryptionService;

    public UserKeyQueryService(UserKeyRepository userKeyRepository,
                               UserRepository userRepository,
                               AesGcmKeyEncryptionService keyEncryptionService) {
        this.userKeyRepository = userKeyRepository;
        this.userRepository = userRepository;
        this.keyEncryptionService = keyEncryptionService;
    }

    @Transactional(readOnly = true)
    public List<PublicUserKeyResponse> listPublicKeys() {
        return userKeyRepository.findAllWithUser().stream()
                .map(userKey -> new PublicUserKeyResponse(
                        userKey.getUser().getId(),
                        userKey.getUser().getEmail(),
                        userKey.getPublicKey()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyUserKeyResponse getMyKeys(String authenticatedEmail) {
        String normalizedEmail = authenticatedEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        UserKey userKey = userKeyRepository.findByUserId(user.getId())
                .orElseThrow(() -> new MissingUserKeyException(user.getId()));

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(keyEncryptionService.decryptPrivateKey(userKey.getPrivateKeyEncrypted()));

        return new MyUserKeyResponse(
                user.getId(),
                user.getEmail(),
                userKey.getPublicKey(),
                privateKeyBase64
        );
    }
}
