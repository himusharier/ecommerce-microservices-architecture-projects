package com.himusharier.auth.service;
import com.himusharier.auth.exception.RefreshTokenException;
import com.himusharier.auth.model.RefreshToken;
import com.himusharier.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Service
public class RefreshTokenService {
    @Value("${app.jwt.refreshExpiration:604800000}") // Default: 7 days in milliseconds
    private long refreshTokenDurationMs;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(UUID userId, String deviceInfo, String ipAddress, String userAgent) {
        // For multi-device support: DO NOT delete existing tokens
        // Each device gets its own refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public List<RefreshToken> findAllByUserId(UUID userId) {
        return refreshTokenRepository.findAllByUserId(userId);
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
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllByUserId(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
