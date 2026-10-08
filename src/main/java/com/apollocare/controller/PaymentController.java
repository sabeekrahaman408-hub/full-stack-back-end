package com.apollocare.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.PaymentResponse;
import com.apollocare.dto.StatusRequest;
import com.apollocare.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentResponse> all() {
        return paymentService.findAll();
    }

    @GetMapping("/appointment/{appointmentId}")
    public PaymentResponse forAppointment(@PathVariable Long appointmentId, Authentication authentication) {
        return paymentService.findByAppointment(appointmentId, authentication.getName(),
                AppointmentController.isAdmin(authentication));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return paymentService.updateStatus(id, request.status());
    }
}
