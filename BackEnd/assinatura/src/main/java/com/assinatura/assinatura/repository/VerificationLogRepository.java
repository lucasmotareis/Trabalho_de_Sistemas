package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
}
