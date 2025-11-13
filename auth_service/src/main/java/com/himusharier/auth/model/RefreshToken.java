package com.himusharier.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "em_refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private Instant createdAt;

    // Device information for multi-device support
    @Column(length = 500)
    private String deviceInfo;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}

