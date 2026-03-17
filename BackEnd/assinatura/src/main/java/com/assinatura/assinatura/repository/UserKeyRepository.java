package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserKeyRepository extends JpaRepository<UserKey, Long> {
}
