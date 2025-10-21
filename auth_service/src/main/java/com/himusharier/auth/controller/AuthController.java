package com.himusharier.auth.controller;

import com.himusharier.auth.config.JwtTokenProvider;
import com.himusharier.auth.dto.request.LoginRequest;
import com.himusharier.auth.dto.request.RefreshTokenRequest;
import com.himusharier.auth.dto.request.RegisterRequest;
import com.himusharier.auth.dto.response.AuthResponse;
import com.himusharier.auth.dto.response.TokenRefreshResponse;
import com.himusharier.auth.exception.LoginRequestException;
import com.himusharier.auth.exception.RegisterRequestException;
import com.himusharier.auth.model.Auth;
import com.himusharier.auth.model.AuthUserDetails;
import com.himusharier.auth.service.JwtAuthService;
import com.himusharier.auth.service.TokenBlacklistService;
import com.himusharier.auth.service.RefreshTokenService;
import com.himusharier.auth.model.RefreshToken;
import com.himusharier.auth.exception.RefreshTokenException;
import com.himusharier.auth.util.ApiResponse;
import com.himusharier.auth.util.JwtExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
//@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          JwtAuthService userService,
                          TokenBlacklistService tokenBlacklistService,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            Auth auth = Auth.builder()
                    .email(registerRequest.email())
                    .password(registerRequest.password())
                    .userRole(registerRequest.userRole())
                    .build();

            Auth savedAuth = userService.createAuth(auth);

            ApiResponse<Auth> response = new ApiResponse<>(
                    true, //true
                    "Registration successful!"
            );

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (MethodArgumentTypeMismatchException e) {
            throw new RegisterRequestException(e.getMessage());

        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse,
                                              @Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.createToken(authentication);

            // Get user details
            AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();
            Auth auth = userDetails.auth();

            // Add user information
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", auth.getUserId());
            userData.put("email", auth.getEmail());
            userData.put("role", auth.getUserRole());
            userData.put("access_token", jwt);
            userData.put("tokenType", "Bearer");

            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(auth.getUserId());
            userData.put("refresh_token", refreshToken.getToken());

            ApiResponse<Object> response = new ApiResponse<>(
                    true, //true
                    "Authentication successful!",
                    userData
            );

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (AuthenticationException e) {
            throw new LoginRequestException(e.getMessage());
        }
    }

    // This endpoint can be called from Angular to check if a token is valid
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        JwtExtractor jwtExtractor = new JwtExtractor();
        String jwt = jwtExtractor.getJwtFromRequest(request);

        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                ApiResponse<String> response = new ApiResponse<>(
                        false,
                        "Token has been invalidated. Please login again."
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Return user information
            AuthUserDetails customUserDetails = (AuthUserDetails) userDetails;
            Auth auth = customUserDetails.auth();

            AuthResponse authResponse = new AuthResponse();
            authResponse.setUserId(auth.getUserId());
            authResponse.setEmail(auth.getEmail());
            authResponse.setUserRole(auth.getUserRole());
            authResponse.setCreatedAt(auth.getCreatedAt());

            ApiResponse<AuthResponse> response = new ApiResponse<>(
                    true, //true
                    "Authorization successful!",
                    authResponse
            );

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        ApiResponse<String> response = new ApiResponse<>(
                false, //true
                "Authorization failed."
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();
        try {
            RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                    .orElseThrow(() -> new RefreshTokenException("Refresh token not found. Please login again."));
            // Verify the refresh token is not expired
            refreshToken = refreshTokenService.verifyExpiration(refreshToken);
            // Get user details
            Auth auth = userService.getAuthByUserId(refreshToken.getUserId())
                    .orElseThrow(() -> new RefreshTokenException("User not found for this refresh token."));
            // Create new access token
            String newAccessToken = jwtTokenProvider.createTokenFromAuth(auth);
            // Create new refresh token
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(auth.getUserId());
            TokenRefreshResponse tokenRefreshResponse = new TokenRefreshResponse(
                    newAccessToken,
                    newRefreshToken.getToken()
            );
            ApiResponse<TokenRefreshResponse> response = new ApiResponse<>(
                    true,
                    "Token refreshed successfully!",
                    tokenRefreshResponse
            );
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (RefreshTokenException e) {
            ApiResponse<String> response = new ApiResponse<>(
                    false,
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        JwtExtractor jwtExtractor = new JwtExtractor();
        String jwt = jwtExtractor.getJwtFromRequest(request);

        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            // Get user details first
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (userDetails != null) {
                AuthUserDetails customUserDetails = (AuthUserDetails) userDetails;
                UUID userId = customUserDetails.getId();

                // Add token to database blacklist
                tokenBlacklistService.blacklistToken(jwt, userId);

                // Delete refresh token for the user
                refreshTokenService.deleteByUserId(userId);
            }
            
            // Clear security context
            SecurityContextHolder.clearContext();

            ApiResponse<String> response = new ApiResponse<>(
                    true,
                    "Logout successful!"
            );

            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        ApiResponse<String> response = new ApiResponse<>(
                false,
                "Invalid token or already logged out."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}