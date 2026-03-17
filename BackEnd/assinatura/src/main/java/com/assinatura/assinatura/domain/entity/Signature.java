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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "signatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_signatures_user")
    )
    private User user;

    @Lob
    @Column(name = "original_text", nullable = false)
    private String originalText;

    @Column(name = "text_hash", nullable = false, length = 512)
    private String textHash;

    @Lob
    @Column(name = "signature_base64", nullable = false)
    private String signatureBase64;

    @Column(name = "hash_algorithm", nullable = false, length = 100)
    private String hashAlgorithm;

    @Column(name = "signature_algorithm", nullable = false, length = 100)
    private String signatureAlgorithm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "signature")
    private List<VerificationLog> verificationLogs = new ArrayList<>();
}