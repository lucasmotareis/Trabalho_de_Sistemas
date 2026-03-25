package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.dto.AdminVerificationLogResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.service.VerificationLogQueryService;
import com.assinatura.assinatura.service.auth.SessionAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminVerificationLogController {

    private static final String AUTHENTICATED_EMAIL_SESSION_KEY = "AUTHENTICATED_EMAIL";

    private final VerificationLogQueryService verificationLogQueryService;
    private final SessionAuthService sessionAuthService;

    public AdminVerificationLogController(VerificationLogQueryService verificationLogQueryService,
                                          SessionAuthService sessionAuthService) {
        this.verificationLogQueryService = verificationLogQueryService;
        this.sessionAuthService = sessionAuthService;
    }

    @GetMapping("/verification-logs")
    public ResponseEntity<List<AdminVerificationLogResponse>> listAllVerificationLogs(HttpServletRequest request) {
        String authenticatedEmail = resolveAuthenticatedEmail(request);
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new InvalidCredentialsException();
        }
        if (!sessionAuthService.isAdmin(authenticatedEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(verificationLogQueryService.listAllVerificationLogs());
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
