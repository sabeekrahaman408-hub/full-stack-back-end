package com.apollocare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apollocare.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    void deleteByDoctorId(Long doctorId);

    @Query("select avg(r.rating) from Review r where r.doctor.id = :doctorId")
    Double averageRating(@Param("doctorId") Long doctorId);
}
