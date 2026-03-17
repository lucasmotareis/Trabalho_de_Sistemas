package com.assinatura.assinatura.service.auth;

import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.dto.AuthUserResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionAuthService {

    private final Map<String, String> authenticatedSessions = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SessionAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public AuthUserResponse getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        return toResponse(user);
    }

    public AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getNome(), user.getEmail());
    }

    public void registerAuthenticatedSession(String sessionId, String email) {
        authenticatedSessions.put(sessionId, email);
    }

    public String findAuthenticatedEmailBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return authenticatedSessions.get(sessionId);
    }

    public void invalidateSession(String sessionId) {
        if (sessionId != null) {
            authenticatedSessions.remove(sessionId);
        }
    }
}
