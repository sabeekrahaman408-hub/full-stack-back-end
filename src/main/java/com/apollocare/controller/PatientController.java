package com.apollocare.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import com.apollocare.dto.MedicalRecordRequest;
import com.apollocare.dto.MedicalRecordResponse;
import com.apollocare.dto.PasswordChangeRequest;
import com.apollocare.dto.PatientRecordRequest;
import com.apollocare.dto.ProfileRequest;
import com.apollocare.dto.UserResponse;
import com.apollocare.service.PatientService;
import com.apollocare.service.RecordFileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;
    private final RecordFileService recordFileService;

    public PatientController(PatientService patientService, RecordFileService recordFileService) {
        this.patientService = patientService;
        this.recordFileService = recordFileService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return patientService.profile(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody ProfileRequest request, Authentication authentication) {
        return patientService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/me/password")
    public Map<String, String> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                              Authentication authentication) {
        patientService.changePassword(authentication.getName(), request);
        return Map.of("message", "Password updated");
    }

    @GetMapping("/me/records")
    public List<MedicalRecordResponse> myRecords(Authentication authentication) {
        return patientService.myRecords(authentication.getName());
    }

    /** Patient adds their own record (allergy, lab report, old prescription ...). */
    @PostMapping("/me/records")
    public ResponseEntity<MedicalRecordResponse> addMyRecord(@Valid @RequestBody PatientRecordRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.addMyRecord(authentication.getName(), request));
    }

    /** multipart/form-data: file + title, category, doctorName, recordDate, prescription, notes, analyze */
    @PostMapping(value = "/me/records/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalRecordResponse> upload(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String title,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) String doctorName,
                                                        @RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
                                                        @RequestParam(required = false) String prescription,
                                                        @RequestParam(required = false) String notes,
                                                        @RequestParam(defaultValue = "false") boolean analyze,
                                                        Authentication authentication) {
        MedicalRecordResponse created = recordFileService.upload(authentication.getName(), file, title, category,
                doctorName, recordDate, prescription, notes, analyze);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Read the attached file again with AI (e.g. after the key was added, or if it failed). */
    @PostMapping("/me/records/{id}/analyze")
    public MedicalRecordResponse analyze(@PathVariable Long id, Authentication authentication) {
        return recordFileService.reanalyze(authentication.getName(), id);
    }

    @GetMapping("/me/records/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, Authentication authentication) {
        RecordFileService.Download d = recordFileService.download(authentication.getName(), id, false);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(d.resource());
    }

    /** Admin can open any patient's file. */
    @GetMapping("/records/{id}/file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> adminFile(@PathVariable Long id, Authentication authentication) {
        RecordFileService.Download d = recordFileService.download(authentication.getName(), id, true);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(d.resource());
    }

    @PutMapping("/me/records/{id}")
    public MedicalRecordResponse updateMyRecord(@PathVariable Long id,
                                                @Valid @RequestBody PatientRecordRequest request,
                                                Authentication authentication) {
        return patientService.updateMyRecord(authentication.getName(), id, request);
    }

    @DeleteMapping("/me/records/{id}")
    public ResponseEntity<Void> deleteMyRecord(@PathVariable Long id, Authentication authentication) {
        patientService.deleteMyRecord(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> all() {
        return patientService.allPatients();
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MedicalRecordResponse> records(@PathVariable Long id) {
        return patientService.recordsForPatient(id);
    }

    @PostMapping("/records")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalRecordResponse> addRecord(@Valid @RequestBody MedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.addRecord(request));
    }
}
