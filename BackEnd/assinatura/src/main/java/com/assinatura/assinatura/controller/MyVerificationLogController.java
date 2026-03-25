package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.dto.MyVerificationLogResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.service.VerificationLogQueryService;
import com.assinatura.assinatura.service.auth.SessionAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MyVerificationLogController {

    private static final String AUTHENTICATED_EMAIL_SESSION_KEY = "AUTHENTICATED_EMAIL";

    private final VerificationLogQueryService verificationLogQueryService;
    private final SessionAuthService sessionAuthService;

    public MyVerificationLogController(VerificationLogQueryService verificationLogQueryService,
                                       SessionAuthService sessionAuthService) {
        this.verificationLogQueryService = verificationLogQueryService;
        this.sessionAuthService = sessionAuthService;
    }

    @GetMapping("/me/verification-logs")
    public ResponseEntity<List<MyVerificationLogResponse>> listMyVerificationLogs(HttpServletRequest request) {
        String authenticatedEmail = resolveAuthenticatedEmail(request);
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return ResponseEntity.ok(verificationLogQueryService.listMyVerificationLogs(authenticatedEmail));
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
