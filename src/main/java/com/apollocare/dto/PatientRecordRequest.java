package com.apollocare.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A medical record the patient adds about themselves (allergy, lab report, old prescription, surgery ...). */
public record PatientRecordRequest(
        @NotBlank(message = "Title is required") @Size(max = 300, message = "Title is too long") String title,
        String category,
        String doctorName,
        LocalDate recordDate,
        @Size(max = 2000, message = "Prescription / details are too long") String prescription,
        @Size(max = 2000, message = "Notes are too long") String notes) {
}
