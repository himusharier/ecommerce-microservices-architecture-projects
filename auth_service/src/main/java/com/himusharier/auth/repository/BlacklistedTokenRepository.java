package com.himusharier.auth.repository;

import com.himusharier.auth.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    Optional<BlacklistedToken> findByToken(String token);

    boolean existsByToken(String token);

    List<BlacklistedToken> findAllByExpiresAtBefore(Instant expiryDate);
}

