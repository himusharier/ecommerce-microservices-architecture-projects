package com.himusharier.auth.controller;

import com.himusharier.auth.config.JwtTokenProvider;
import com.himusharier.auth.dto.request.LoginRequest;
import com.himusharier.auth.dto.request.RegisterRequest;
import com.himusharier.auth.dto.response.AuthResponse;
import com.himusharier.auth.exception.LoginRequestException;
import com.himusharier.auth.exception.RegisterRequestException;
import com.himusharier.auth.model.Auth;
import com.himusharier.auth.model.AuthUserDetails;
import com.himusharier.auth.service.JwtAuthService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
//@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          JwtAuthService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
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

            Map<String, Object> authResponse = new HashMap<>();
            authResponse.put("status", "success");
            authResponse.put("code", HttpStatus.OK.value());
            authResponse.put("message", "Registration successful. Please login...");

            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);

        } catch (MethodArgumentTypeMismatchException e) {
            throw new RegisterRequestException(e.getMessage());

        } catch (RuntimeException e) {
            throw new RegisterRequestException(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(HttpServletRequest request,
                                              HttpServletResponse response,
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

            // Create response with token and user info
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("code", HttpStatus.OK.value());
            responseData.put("message", "Login successful");

            responseData.put("access_token", jwt);
            responseData.put("tokenType", "Bearer");

            // Add user information
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", auth.getUserId());
            userData.put("email", auth.getEmail());
            userData.put("role", auth.getUserRole());

            responseData.put("user", userData);

            return ResponseEntity.ok(responseData);

        } catch (AuthenticationException e) {
            throw new LoginRequestException(e.getMessage());
        }
    }

    // This endpoint can be called from Angular to check if a token is valid
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);

        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Return user information
            AuthUserDetails customUserDetails = (AuthUserDetails) userDetails;
            Auth auth = customUserDetails.auth();

            AuthResponse authResponse = new AuthResponse();
            authResponse.setUserId(auth.getUserId());
            authResponse.setEmail(auth.getEmail());
            authResponse.setUserRole(auth.getUserRole());

            return ResponseEntity.ok(authResponse);
        }

        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("status", "error");
        authResponse.put("code", HttpStatus.UNAUTHORIZED.value());
        authResponse.put("message", "Invalid or expired token.");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authResponse);
    }

    // Helper method to extract JWT from request
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}