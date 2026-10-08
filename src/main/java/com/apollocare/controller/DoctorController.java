package com.apollocare.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.DoctorRequest;
import com.apollocare.dto.ReviewRequest;
import com.apollocare.dto.ReviewResponse;
import com.apollocare.dto.SlotResponse;
import com.apollocare.entity.Doctor;
import com.apollocare.service.DoctorService;
import com.apollocare.service.SlotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final SlotService slotService;

    public DoctorController(DoctorService doctorService, SlotService slotService) {
        this.doctorService = doctorService;
        this.slotService = slotService;
    }

    /** GET /doctors?q=heart&specialization=Cardiologist&location=Chennai (all params optional). */
    @GetMapping
    public List<Doctor> getAll(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String specialization,
                               @RequestParam(required = false) String location) {
        return doctorService.search(q, specialization, location);
    }

    /** Values for the filter dropdowns on the doctors page. */
    @GetMapping("/filters")
    public Map<String, List<String>> filters() {
        return doctorService.filterOptions();
    }

    @GetMapping("/{id}")
    public Doctor getOne(@PathVariable Long id) {
        return doctorService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Doctor> create(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Doctor update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /doctors/3/slots?date=2026-10-08  ->  every slot with available true/false. */
    @GetMapping("/{id}/slots")
    public List<SlotResponse> slots(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.slotsFor(id, date);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long id) {
        return doctorService.reviews(id);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable Long id,
                                                    @Valid @RequestBody ReviewRequest request,
                                                    Authentication authentication) {
        ReviewResponse created = doctorService.addReview(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
