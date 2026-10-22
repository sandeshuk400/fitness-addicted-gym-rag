package com.fitnessaddicted.gym.auth.repository;

import java.util.Optional;

import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
