package com.assinatura.assinatura.controller;

import com.assinatura.assinatura.dto.VerifyRequest;
import com.assinatura.assinatura.dto.VerificationResponse;
import com.assinatura.assinatura.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<VerificationResponse> verifyByPublicId(@PathVariable("publicId") String publicId) {
        return ResponseEntity.ok(verificationService.verifyByPublicId(publicId));
    }

    @PostMapping
    public ResponseEntity<VerificationResponse> verifyManual(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(verificationService.verifyManual(request));
    }
}
