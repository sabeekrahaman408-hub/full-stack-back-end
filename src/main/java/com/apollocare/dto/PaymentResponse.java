package com.apollocare.dto;

import java.time.LocalDateTime;

import com.apollocare.entity.Payment;

public record PaymentResponse(
        Long id,
        Long appointmentId,
        String patientEmail,
        Double amount,
        String method,
        String status,
        String transactionId,
        String upiId,
        String cardLast4,
        String cardHolder,
        LocalDateTime createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getAppointmentId(), p.getPatientEmail(), p.getAmount(),
                p.getMethod(), p.getStatus(), p.getTransactionId(), p.getUpiId(), p.getCardLast4(),
                p.getCardHolder(), p.getCreatedAt());
    }
}
