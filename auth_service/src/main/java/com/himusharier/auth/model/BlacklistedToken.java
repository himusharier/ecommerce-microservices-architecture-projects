package com.himusharier.auth.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "em_blacklisted_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false, unique = true, length = 1000)
    private String token;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private Instant blacklistedAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @PrePersist
    protected void onCreate() {
        blacklistedAt = Instant.now();
    }
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
