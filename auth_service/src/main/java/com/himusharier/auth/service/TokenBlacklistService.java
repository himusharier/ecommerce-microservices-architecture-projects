package com.himusharier.auth.service;
import com.himusharier.auth.model.BlacklistedToken;
import com.himusharier.auth.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
@Service
public class TokenBlacklistService {
    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;
    @Autowired
    private com.himusharier.auth.config.JwtTokenProvider jwtTokenProvider;
    @Transactional
    public void blacklistToken(String token, UUID userId) {
        // Extract expiration time from token
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiration.toInstant())
                .build();
        blacklistedTokenRepository.save(blacklistedToken);
    }
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }
    @Transactional
    public void cleanupExpiredTokens() {
        List<BlacklistedToken> expiredTokens = blacklistedTokenRepository
                .findAllByExpiresAtBefore(Instant.now());
        blacklistedTokenRepository.deleteAll(expiredTokens);
    }
}

