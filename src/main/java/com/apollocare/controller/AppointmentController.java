package com.apollocare.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.AppointmentRequest;
import com.apollocare.dto.AppointmentResponse;
import com.apollocare.dto.CancelRequest;
import com.apollocare.dto.StatusRequest;
import com.apollocare.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** Patients: their own appointments. Admins: everything (or ?email= to filter). */
    @GetMapping
    public List<AppointmentResponse> list(@RequestParam(required = false) String email,
                                          Authentication authentication) {
        return appointmentService.list(authentication.getName(), isAdmin(authentication), email);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id, Authentication authentication) {
        return appointmentService.get(id, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request,
                                                      Authentication authentication) {
        AppointmentResponse created = appointmentService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Admin approves / rejects / completes an appointment. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AppointmentResponse patchStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return appointmentService.updateStatus(id, request.status(), request.note());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AppointmentResponse putStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return appointmentService.updateStatus(id, request.status(), request.note());
    }

    /** Patient cancels own appointment (admin can cancel any). Body is optional: {"reason": "..."} */
    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id,
                                      @RequestBody(required = false) CancelRequest request,
                                      Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return appointmentService.cancel(id, authentication.getName(), isAdmin(authentication), reason);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        appointmentService.delete(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }
}
