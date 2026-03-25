package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.domain.entity.User;
import com.assinatura.assinatura.dto.AuthUserResponse;
import com.assinatura.assinatura.dto.LoginRequest;
import com.assinatura.assinatura.dto.SignupRequest;
import com.assinatura.assinatura.dto.SignupResponse;
import com.assinatura.assinatura.exception.InvalidCredentialsException;
import com.assinatura.assinatura.service.auth.SessionAuthService;
import com.assinatura.assinatura.service.SignupService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTHENTICATED_EMAIL_SESSION_KEY = "AUTHENTICATED_EMAIL";
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    private final SignupService signupService;
    private final SessionAuthService sessionAuthService;

    public AuthController(SignupService signupService, SessionAuthService sessionAuthService) {
        this.signupService = signupService;
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = signupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        User user = sessionAuthService.authenticate(request.email(), request.password());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                sessionAuthService.authoritiesFor(user)
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
        session.setAttribute(AUTHENTICATED_EMAIL_SESSION_KEY, user.getEmail());
        sessionAuthService.registerAuthenticatedSession(session.getId(), user.getEmail());
        httpResponse.addCookie(new Cookie("JSESSIONID", session.getId()));

        return ResponseEntity.ok(sessionAuthService.toResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String authenticatedEmail = null;
        if (session != null) {
            authenticatedEmail = (String) session.getAttribute(AUTHENTICATED_EMAIL_SESSION_KEY);
        }
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            String sessionId = getSessionIdFromCookie(request);
            authenticatedEmail = sessionAuthService.findAuthenticatedEmailBySessionId(sessionId);
            if ((authenticatedEmail == null || authenticatedEmail.isBlank()) && sessionId != null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    authenticatedEmail = authentication.getName();
                }
            }
            if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
                throw new InvalidCredentialsException();
            }
        }

        AuthUserResponse currentUser = sessionAuthService.getCurrentUser(authenticatedEmail);
        return ResponseEntity.ok(currentUser);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionAuthService.invalidateSession(session.getId());
            session.invalidate();
        }
        sessionAuthService.invalidateSession(getSessionIdFromCookie(request));
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
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
