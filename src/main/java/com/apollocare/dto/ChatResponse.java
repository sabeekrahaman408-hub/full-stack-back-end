package com.apollocare.dto;

/** mode: "ai" (answered by Claude), "faq" (built-in answers), "emergency" (safety message). */
public record ChatResponse(String reply, String mode) {
}
