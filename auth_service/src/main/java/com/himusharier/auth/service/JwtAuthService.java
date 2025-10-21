package com.himusharier.auth.service;

import com.himusharier.auth.constants.UserRole;
import com.himusharier.auth.model.Auth;
import com.himusharier.auth.model.AuthUserDetails;
import com.himusharier.auth.repository.AuthRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtAuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public JwtAuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Auth createAuth(Auth auth) {
        if (authRepository.existsByEmail(auth.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        auth.setPassword(passwordEncoder.encode(auth.getPassword()));
        auth.setActive(true);

        /*if (auth.getUserRole() == null) {
            auth.setUserRole(UserRole.CUSTOMER);
        }*/

        Auth saveAuth = authRepository.save(auth);


        return saveAuth;
    }

        public Optional<Auth> getAuthByUserId(UUID userId) {
        return authRepository.findById(userId);
    }

    public UserDetails loadUserByUsername(String username) {
        Optional<Auth> byEmail = authRepository.findByEmail(username);
        return byEmail.map(AuthUserDetails::new).orElse(null);
    }
}

