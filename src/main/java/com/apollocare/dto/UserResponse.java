package com.apollocare.dto;

import java.time.LocalDateTime;

import com.apollocare.entity.User;

public record UserResponse(Long id, String name, String email, String role, String phone,
                           LocalDateTime createdAt, LocalDateTime lastLoginAt, Long doctorId) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name().toLowerCase(),
                u.getPhone(), u.getCreatedAt(), u.getLastLoginAt(), u.getDoctorId());
    }
}
