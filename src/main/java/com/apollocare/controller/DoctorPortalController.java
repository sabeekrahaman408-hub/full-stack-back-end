package com.apollocare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.AppointmentResponse;
import com.apollocare.dto.ConsultationRequest;
import com.apollocare.dto.MedicalRecordResponse;
import com.apollocare.dto.StatusRequest;
import com.apollocare.service.DoctorPortalService;

import jakarta.validation.Valid;

/** Doctor panel API. Requires the DOCTOR role (see SecurityConfig: /doctor/**). */
@RestController
@RequestMapping("/doctor")
public class DoctorPortalController {

    private final DoctorPortalService portalService;

    public DoctorPortalController(DoctorPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(Authentication authentication) {
        return portalService.overview(authentication.getName());
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments(Authentication authentication) {
        return portalService.appointments(authentication.getName());
    }

    /** Body: {"status": "approved" | "rejected" | "completed" | "no-show" | "cancelled", "note": "optional"} */
    @PatchMapping("/appointments/{id}")
    public AppointmentResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request,
                                            Authentication authentication) {
        return portalService.updateStatus(authentication.getName(), id, request.status(), request.note());
    }

    @PostMapping("/appointments/{id}/consultation")
    public ResponseEntity<MedicalRecordResponse> consultation(@PathVariable Long id,
                                                              @Valid @RequestBody ConsultationRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.addConsultation(authentication.getName(), id, request));
    }
}
