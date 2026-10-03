package com.microvault.auth.repository;

import com.microvault.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedFalse(UUID id);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseAndDeletedFalse(String email);

    List<User> findByDeletedFalseOrderByCreatedAtDesc();

    List<User> findByRoleIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(String role);
}
