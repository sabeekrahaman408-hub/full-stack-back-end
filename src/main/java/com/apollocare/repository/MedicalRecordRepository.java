package com.apollocare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apollocare.entity.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
