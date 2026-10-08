package com.apollocare.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apollocare.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findFirstByNameIgnoreCase(String name);
}
