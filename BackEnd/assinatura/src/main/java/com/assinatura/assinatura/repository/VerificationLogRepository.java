package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    @Query("""
            SELECT vl
            FROM VerificationLog vl
            JOIN FETCH vl.signature
            WHERE vl.verifiedByUser.id = :userId
            ORDER BY vl.verifiedAt DESC, vl.id DESC
            """)
    List<VerificationLog> findAllByVerifiedByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT vl
            FROM VerificationLog vl
            JOIN FETCH vl.signature
            LEFT JOIN FETCH vl.verifiedByUser
            ORDER BY vl.verifiedAt DESC, vl.id DESC
            """)
    List<VerificationLog> findAllWithSignatureAndVerifier();
}
