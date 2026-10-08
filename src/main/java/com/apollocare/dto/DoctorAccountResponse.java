package com.apollocare.dto;

public record DoctorAccountResponse(Long doctorId, String doctorName, String email, boolean hasLogin) {
}
