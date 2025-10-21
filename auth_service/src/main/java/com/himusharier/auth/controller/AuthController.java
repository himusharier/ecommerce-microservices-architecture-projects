package com.himusharier.auth.controller;

import com.himusharier.auth.config.JwtTokenProvider;
import com.himusharier.auth.constants.UserRole;
import com.himusharier.auth.dto.request.LoginRequest;
import com.himusharier.auth.dto.request.RegisterRequest;
import com.himusharier.auth.dto.response.AuthResponse;
import com.himusharier.auth.exception.LoginRequestException;
import com.himusharier.auth.exception.RegisterRequestException;
import com.himusharier.auth.model.Auth;
import com.himusharier.auth.model.AuthUserDetails;
import com.himusharier.auth.service.JwtAuthService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
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

            ApiResponse<Auth> response = new ApiResponse<>(
                    true, //true
                    "Registration successful!"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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
}