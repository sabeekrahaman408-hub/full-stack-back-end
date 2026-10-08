package com.apollocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.MedicalRecordRequest;
import com.apollocare.dto.MedicalRecordResponse;
import com.apollocare.dto.PasswordChangeRequest;
import com.apollocare.dto.PatientRecordRequest;
import com.apollocare.dto.ProfileRequest;
import com.apollocare.dto.UserResponse;
import com.apollocare.entity.MedicalRecord;
import com.apollocare.entity.Role;
import com.apollocare.entity.User;
import com.apollocare.exception.BadRequestException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.MedicalRecordRepository;
import com.apollocare.repository.UserRepository;

@Service
public class PatientService {

    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorage;

    public PatientService(UserRepository userRepository,
                          MedicalRecordRepository medicalRecordRepository,
                          PasswordEncoder passwordEncoder,
                          FileStorageService fileStorage) {
        this.userRepository = userRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public UserResponse updateProfile(String email, ProfileRequest request) {
        User user = findByEmail(email);
        user.setName(request.name().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse profile(String email) {
        return UserResponse.from(findByEmail(email));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> allPatients() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.PATIENT).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> myRecords(String email) {
        User user = findByEmail(email);
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MedicalRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> recordsForPatient(Long patientId) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id " + patientId);
        }
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(MedicalRecordResponse::from).toList();
    }

    @Transactional
    public MedicalRecordResponse addRecord(MedicalRecordRequest request) {
        User patient = userRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + request.patientId()));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setSource("doctor");
        record.setAppointmentId(request.appointmentId());
        record.setDoctorName(request.doctorName());
        record.setDiagnosis(request.diagnosis());
        record.setPrescription(request.prescription());
        record.setNotes(request.notes());
        return MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    private static final Set<String> CATEGORIES = Set.of(
            "Allergy", "Lab report", "Prescription", "Vaccination", "Surgery", "Chronic condition", "Other");

    @Transactional
    public MedicalRecordResponse addMyRecord(String email, PatientRecordRequest request) {
        User patient = findByEmail(email);
        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setSource("patient");
        apply(record, request);
        return MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    @Transactional
    public MedicalRecordResponse updateMyRecord(String email, Long id, PatientRecordRequest request) {
        MedicalRecord record = ownedPatientRecord(email, id);
        apply(record, request);
        return MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    @Transactional
    public void deleteMyRecord(String email, Long id) {
        MedicalRecord record = ownedPatientRecord(email, id);
        String fileKey = record.getFileKey();
        medicalRecordRepository.delete(record);
        fileStorage.deleteQuietly(fileKey);
    }

    /** Patients may only change records THEY added - never a diagnosis written by a doctor. */
    private MedicalRecord ownedPatientRecord(String email, Long id) {
        User patient = findByEmail(email);
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id " + id));
        if (!record.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("You can only change your own records");
        }
        if (!"patient".equals(record.getSource())) {
            throw new AccessDeniedException("Records written by your doctor cannot be edited or deleted");
        }
        return record;
    }

    private void apply(MedicalRecord record, PatientRecordRequest request) {
        String category = request.category() == null || request.category().isBlank() ? "Other" : request.category().trim();
        if (!CATEGORIES.contains(category)) {
            throw new BadRequestException("Category must be one of " + CATEGORIES);
        }
        if (request.recordDate() != null && request.recordDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Record date cannot be in the future");
        }
        record.setDiagnosis(request.title().trim());
        record.setCategory(category);
        record.setDoctorName(request.doctorName() == null || request.doctorName().isBlank() ? null : request.doctorName().trim());
        record.setRecordDate(request.recordDate());
        record.setPrescription(request.prescription());
        record.setNotes(request.notes());
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
