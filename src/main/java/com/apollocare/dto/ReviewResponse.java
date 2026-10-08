package com.apollocare.dto;

import java.time.LocalDateTime;

import com.apollocare.entity.Review;

public record ReviewResponse(Long id, Long doctorId, String patientName, Integer rating, String comment,
                             LocalDateTime createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getDoctor().getId(), r.getPatient().getName(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
