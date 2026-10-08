package com.apollocare.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.apollocare.entity.MedicalRecord;

public record MedicalRecordResponse(Long id, Long patientId, String patientName, Long appointmentId,
                                    String doctorName, String diagnosis, String prescription, String notes,
                                    String source, String category, LocalDate recordDate,
                                    boolean hasFile, String fileName, String fileType, Long fileSize,
                                    String aiStatus, String aiSummary, LocalDateTime aiAnalyzedAt,
                                    LocalDateTime createdAt) {

    public static MedicalRecordResponse from(MedicalRecord m) {
        String source = m.getSource() == null ? "doctor" : m.getSource();
        boolean hasFile = m.getFileKey() != null;
        return new MedicalRecordResponse(m.getId(), m.getPatient().getId(), m.getPatient().getName(),
                m.getAppointmentId(), m.getDoctorName(), m.getDiagnosis(), m.getPrescription(),
                m.getNotes(), source, m.getCategory(), m.getRecordDate(),
                hasFile, m.getFileName(), m.getFileType(), m.getFileSize(),
                hasFile ? (m.getAiStatus() == null ? "none" : m.getAiStatus()) : null,
                m.getAiSummary(), m.getAiAnalyzedAt(), m.getCreatedAt());
    }
}
