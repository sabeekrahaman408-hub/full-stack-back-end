package com.apollocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DoctorRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Specialization is required") String specialization,
        @PositiveOrZero(message = "Experience cannot be negative") Integer experience,
        @NotNull(message = "Fees is required") @PositiveOrZero(message = "Fees cannot be negative") Double fees,
        @NotBlank(message = "Location is required") String location,
        String image,
        String qualification,
        String about,
        String languages,
        String availableDays) {
}
