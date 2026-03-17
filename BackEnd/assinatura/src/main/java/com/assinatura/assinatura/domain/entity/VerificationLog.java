package com.assinatura.assinatura.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "signature_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_verification_logs_signature")
    )
    private Signature signature;

    @Lob
    @Column(name = "provided_text")
    private String providedText;

    @Lob
    @Column(name = "provided_signature_base64")
    private String providedSignatureBase64;

    @Column(name = "valid", nullable = false)
    private boolean valid;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;
}