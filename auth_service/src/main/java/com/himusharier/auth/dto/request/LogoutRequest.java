package com.himusharier.auth.dto.request;

public record LogoutRequest(
        String refreshToken  // Optional: if provided, will delete this specific refresh token
) {
}

