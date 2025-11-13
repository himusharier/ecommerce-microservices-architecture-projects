package com.himusharier.auth.repository;
import com.himusharier.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserId(UUID userId);
    List<RefreshToken> findAllByUserId(UUID userId);
    void deleteByToken(String token);
    void deleteAllByUserId(UUID userId);
}
