package com.apollocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicalRecordRequest(
        @NotNull(message = "Patient id is required") Long patientId,
        Long appointmentId,
        String doctorName,
        @NotBlank(message = "Diagnosis is required") String diagnosis,
        String prescription,
        String notes) {
}
