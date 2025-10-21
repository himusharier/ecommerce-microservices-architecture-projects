package com.himusharier.auth.service;
import com.himusharier.auth.exception.RefreshTokenException;
import com.himusharier.auth.model.RefreshToken;
import com.himusharier.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
@Service
public class RefreshTokenService {
    @Value("${app.jwt.refreshExpiration:604800000}") // Default: 7 days in milliseconds
    private long refreshTokenDurationMs;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        // Delete existing refresh token for the user (one token per user)
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);
        existingToken.ifPresent(refreshTokenRepository::delete);
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token has expired. Please login again.");
        }
        return token;
    }
    @Transactional
    public void deleteByUserId(UUID userId) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(userId);
        refreshToken.ifPresent(refreshTokenRepository::delete);
    }
    @Transactional
    public void deleteByToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        refreshToken.ifPresent(refreshTokenRepository::delete);
    }
}
