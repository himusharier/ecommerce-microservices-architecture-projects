package com.himusharier.auth.repository;

import com.himusharier.auth.constants.UserRole;
import com.himusharier.auth.model.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<Auth, UUID> {

    Optional<Auth> findByEmail(String email);

    boolean existsByEmail(String email);

}
