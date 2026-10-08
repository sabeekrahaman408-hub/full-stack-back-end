package com.apollocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apollocare.entity.Role;
import com.apollocare.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    long countByRole(Role role);

    Optional<User> findByDoctorId(Long doctorId);

    List<User> findByRole(Role role);
}
