package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignatureRepository extends JpaRepository<Signature, Long> {

    Optional<Signature> findByPublicId(String publicId);
}
