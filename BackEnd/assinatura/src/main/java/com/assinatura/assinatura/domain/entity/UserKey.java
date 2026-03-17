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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_user_keys_user")
    )
    private User user;

    @Column(name = "public_key", nullable = false ,columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String privateKeyEncrypted;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    @Column(name = "key_size", nullable = false)
    private Integer keySize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}