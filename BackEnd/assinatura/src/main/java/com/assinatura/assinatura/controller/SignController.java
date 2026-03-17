package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.dto.SignRequest;
import com.assinatura.assinatura.dto.SignResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.service.SignService;
import com.assinatura.assinatura.service.auth.SessionAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignController {

    private static final String AUTHENTICATED_EMAIL_SESSION_KEY = "AUTHENTICATED_EMAIL";

    private final SignService signService;
    private final SessionAuthService sessionAuthService;

    public SignController(SignService signService, SessionAuthService sessionAuthService) {
        this.signService = signService;
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping("/sign")
    public ResponseEntity<SignResponse> sign(@Valid @RequestBody SignRequest request,
                                             HttpServletRequest httpRequest) {
        String authenticatedEmail = resolveAuthenticatedEmail(httpRequest);
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new InvalidCredentialsException();
        }

        SignResponse response = signService.sign(authenticatedEmail, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
