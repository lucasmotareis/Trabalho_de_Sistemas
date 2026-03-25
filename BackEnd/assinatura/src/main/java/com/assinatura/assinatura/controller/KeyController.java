package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.dto.MyUserKeyResponse;
import com.assinatura.assinatura.dto.PublicUserKeyResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.service.UserKeyQueryService;
import com.assinatura.assinatura.service.auth.SessionAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/keys")
public class KeyController {

    private static final String AUTHENTICATED_EMAIL_SESSION_KEY = "AUTHENTICATED_EMAIL";

    private final UserKeyQueryService userKeyQueryService;
    private final SessionAuthService sessionAuthService;

    public KeyController(UserKeyQueryService userKeyQueryService, SessionAuthService sessionAuthService) {
        this.userKeyQueryService = userKeyQueryService;
        this.sessionAuthService = sessionAuthService;
    }

    @GetMapping("/public")
    public ResponseEntity<List<PublicUserKeyResponse>> listPublicKeys() {
        return ResponseEntity.ok(userKeyQueryService.listPublicKeys());
    }

    @GetMapping("/me")
    public ResponseEntity<MyUserKeyResponse> getMyKeys(HttpServletRequest request) {
        String authenticatedEmail = resolveAuthenticatedEmail(request);
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return ResponseEntity.ok(userKeyQueryService.getMyKeys(authenticatedEmail));
    }

    private String resolveAuthenticatedEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String email = (String) session.getAttribute(AUTHENTICATED_EMAIL_SESSION_KEY);
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        return sessionAuthService.findAuthenticatedEmailBySessionId(getSessionIdFromCookie(request));
    }

    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
