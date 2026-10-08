package com.apollocare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.apollocare.entity.Appointment;

/** Same field names the frontend already uses (doctor, date, time ...). */
public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctor,
        String specialization,
        LocalDate date,
        String time,
        String patientName,
        Integer age,
        String contact,
        String problem,
        String email,
        Double consultationFee,
        String paymentMethod,
        String paymentStatus,
        String status,
        String type,
        String requestedBy,
        String cancelReason,
        String adminNote,
        LocalDateTime createdAt) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(), a.getDoctorId(), a.getDoctorName(), a.getSpecialization(),
                a.getAppointmentDate(), a.getAppointmentTime(), a.getPatientName(), a.getAge(),
                a.getContact(), a.getProblem(), a.getEmail(), a.getConsultationFee(),
                a.getPaymentMethod(), a.getPaymentStatus(), a.getStatus(), a.getType(),
                a.getRequestedBy(), a.getCancelReason(), a.getAdminNote(), a.getCreatedAt());
    }
}
