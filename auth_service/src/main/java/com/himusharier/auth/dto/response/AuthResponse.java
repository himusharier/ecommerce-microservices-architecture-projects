package com.himusharier.auth.dto.response;

import com.himusharier.auth.constants.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AuthResponse {
    private UUID userId;
    private String email;
    private UserRole userRole;
    private LocalDateTime createdAt;
}
