package com.apollocare.dto;

import jakarta.validation.constraints.NotBlank;

/** note is optional (admin note / cancel reason). */
public record StatusRequest(@NotBlank(message = "Status is required") String status, String note) {
}
