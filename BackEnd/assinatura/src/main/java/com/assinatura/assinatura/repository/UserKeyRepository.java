package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserKeyRepository extends JpaRepository<UserKey, Long> {

    Optional<UserKey> findByUserId(Long userId);
}
