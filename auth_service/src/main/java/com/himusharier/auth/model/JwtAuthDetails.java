package com.himusharier.auth.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
public class JwtAuthDetails implements UserDetails {
    private final UUID id;
    private final String email;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtAuthDetails(UUID id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }

}
