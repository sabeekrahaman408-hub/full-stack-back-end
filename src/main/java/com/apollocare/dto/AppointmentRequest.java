package com.apollocare.dto;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mirrors the JSON the booking page sends. Extra fields the page sends
 * (status, createdAt, requestedBy, email ...) are ignored on purpose:
 * the server decides those from the logged-in user.
 *
 * The same rules are checked in the browser; they are repeated here so they cannot be bypassed.
 */
public record AppointmentRequest(
        @NotBlank(message = "Doctor is required") String doctor,
        String specialization,
        @NotNull(message = "Date is required (yyyy-MM-dd)") LocalDate date,
        @NotBlank(message = "Time slot is required") String time,
        @NotBlank(message = "Patient name is required")
        @Pattern(regexp = "^\\p{L}[\\p{L} .'-]{1,49}$", message = "Patient name must contain letters only (2-50 characters, no numbers)")
        String patientName,
        @NotNull(message = "Age is required")
        @Min(value = 1, message = "Age must be between 1 and 99")
        @Max(value = 99, message = "Age must be between 1 and 99")
        Integer age,
        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Contact must be a valid 10-digit mobile number")
        String contact,
        @NotBlank(message = "Please describe the problem")
        @Size(min = 5, max = 500, message = "Problem must be 5 to 500 characters")
        String problem,
        Double consultationFee,
        @NotBlank(message = "Payment method is required") String paymentMethod,
        String type,
        Map<String, String> paymentDetails) {
}
