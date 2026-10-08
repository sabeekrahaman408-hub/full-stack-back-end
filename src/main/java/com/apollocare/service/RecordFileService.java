package com.apollocare.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.apollocare.dto.MedicalRecordResponse;
import com.apollocare.entity.MedicalRecord;
import com.apollocare.entity.User;
import com.apollocare.exception.BadRequestException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.MedicalRecordRepository;
import com.apollocare.repository.UserRepository;

/**
 * Upload, view and AI-read files attached to a patient's own medical records.
 * (Deliberately NOT @Transactional: the AI call can take a while and must not hold a database connection.)
 */
@Service
public class RecordFileService {

    private static final Set<String> CATEGORIES = Set.of(
            "Allergy", "Lab report", "Prescription", "Vaccination", "Surgery", "Chronic condition", "Other");

    private final UserRepository userRepository;
    private final MedicalRecordRepository recordRepository;
    private final FileStorageService storage;
    private final AiAnalysisService ai;

    public RecordFileService(UserRepository userRepository, MedicalRecordRepository recordRepository,
                             FileStorageService storage, AiAnalysisService ai) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.storage = storage;
        this.ai = ai;
    }

    public record Download(Resource resource, String fileName, String mimeType) {
    }

    public MedicalRecordResponse upload(String email, MultipartFile file, String title, String category,
                                        String doctorName, LocalDate recordDate, String prescription,
                                        String notes, boolean analyze) {
        User patient = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String cat = category == null || category.isBlank() ? "Other" : category.trim();
        if (!CATEGORIES.contains(cat)) {
            throw new BadRequestException("Category must be one of " + CATEGORIES);
        }
        if (recordDate != null && recordDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Record date cannot be in the future");
        }
        if (notes != null && notes.length() > 2000 || prescription != null && prescription.length() > 2000) {
            throw new BadRequestException("Notes / details are too long (max 2000 characters)");
        }
        if (title != null && title.length() > 300) {
            throw new BadRequestException("Title is too long (max 300 characters)");
        }

        FileStorageService.StoredFile stored = storage.store(file);

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setSource("patient");
        record.setCategory(cat);
        record.setDiagnosis(title == null || title.isBlank() ? titleFrom(stored.originalName()) : title.trim());
        record.setDoctorName(doctorName == null || doctorName.isBlank() ? null : doctorName.trim());
        record.setRecordDate(recordDate);
        record.setPrescription(prescription);
        record.setNotes(notes);
        record.setFileName(stored.originalName());
        record.setFileKey(stored.key());
        record.setFileType(stored.mimeType());
        record.setFileSize(stored.size());
        record.setAiStatus("none");
        MedicalRecord saved = recordRepository.save(record);

        if (analyze) {
            runAnalysis(saved);
        }
        return MedicalRecordResponse.from(saved);
    }

    public MedicalRecordResponse reanalyze(String email, Long id) {
        MedicalRecord record = owned(email, id);
        if (record.getFileKey() == null) {
            throw new BadRequestException("This record has no file to read");
        }
        runAnalysis(record);
        return MedicalRecordResponse.from(record);
    }

    public Download download(String email, Long id, boolean admin) {
        MedicalRecord record = admin ? recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id " + id)) : owned(email, id);
        if (record.getFileKey() == null) {
            throw new ResourceNotFoundException("This record has no file");
        }
        Path path = storage.resolve(record.getFileKey());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("The file is no longer available on the server");
        }
        return new Download(new PathResource(path), record.getFileName(), record.getFileType());
    }

    private void runAnalysis(MedicalRecord record) {
        AiAnalysisService.Result result = ai.analyze(
                storage.resolve(record.getFileKey()), record.getFileType(), record.getFileName());
        record.setAiStatus(result.status());
        record.setAiSummary(result.text());
        record.setAiAnalyzedAt(LocalDateTime.now());
        recordRepository.save(record);
    }

    private MedicalRecord owned(String email, Long id) {
        User patient = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        MedicalRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id " + id));
        if (!record.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("You can only open your own records");
        }
        return record;
    }

    private static String titleFrom(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        base = base.replace('_', ' ').replace('-', ' ').trim();
        return base.isEmpty() ? "Medical file" : (base.length() > 300 ? base.substring(0, 300) : base);
    }
}
