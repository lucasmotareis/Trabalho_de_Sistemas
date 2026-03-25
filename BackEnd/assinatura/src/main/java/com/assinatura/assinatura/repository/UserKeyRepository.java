package com.assinatura.assinatura.repository;

import com.assinatura.assinatura.domain.entity.UserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserKeyRepository extends JpaRepository<UserKey, Long> {

    Optional<UserKey> findByUserId(Long userId);

    @Query("SELECT uk FROM UserKey uk JOIN FETCH uk.user")
    List<UserKey> findAllWithUser();
}
