package com.himusharier.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class JwtExtractor {
    // Helper method to extract JWT from request
    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
