package com.apollocare.dto;

import jakarta.validation.constraints.NotBlank;

/** Doctor's notes after a visit. Saved to the patient's medical records. */
public record ConsultationRequest(
        @NotBlank(message = "Diagnosis is required") String diagnosis,
        String prescription,
        String notes,
        Boolean markCompleted) {
}
