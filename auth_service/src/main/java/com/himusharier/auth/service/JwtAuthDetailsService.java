package com.himusharier.auth.service;

import com.himusharier.auth.model.Auth;
import com.himusharier.auth.model.AuthUserDetails;
import com.himusharier.auth.repository.AuthRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JwtAuthDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;

    @Autowired
    public JwtAuthDetailsService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + email));

        return new AuthUserDetails(auth);
    }

    @Transactional
    public UserDetails loadUserById(UUID id) {
        Auth auth = authRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return new AuthUserDetails(auth);
    }
}