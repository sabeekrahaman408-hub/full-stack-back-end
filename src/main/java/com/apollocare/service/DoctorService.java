package com.apollocare.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.DoctorRequest;
import com.apollocare.dto.ReviewRequest;
import com.apollocare.dto.ReviewResponse;
import com.apollocare.entity.Doctor;
import com.apollocare.entity.Review;
import com.apollocare.entity.User;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.DoctorRepository;
import com.apollocare.repository.ReviewRepository;
import com.apollocare.repository.UserRepository;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    /** Optional filters; blank/null filter = ignored. q matches name or specialization. */
    @Transactional(readOnly = true)
    public List<Doctor> search(String q, String specialization, String location) {
        String query = q == null ? "" : q.trim().toLowerCase();
        return doctorRepository.findAll().stream()
                .filter(d -> query.isEmpty()
                        || d.getName().toLowerCase().contains(query)
                        || d.getSpecialization().toLowerCase().contains(query))
                .filter(d -> specialization == null || specialization.isBlank()
                        || d.getSpecialization().equalsIgnoreCase(specialization.trim()))
                .filter(d -> location == null || location.isBlank()
                        || (d.getLocation() != null && d.getLocation().equalsIgnoreCase(location.trim())))
                .sorted(Comparator.comparing(Doctor::getId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> filterOptions() {
        TreeSet<String> specializations = new TreeSet<>();
        TreeSet<String> locations = new TreeSet<>();
        for (Doctor d : doctorRepository.findAll()) {
            specializations.add(d.getSpecialization());
            if (d.getLocation() != null && !d.getLocation().isBlank()) {
                locations.add(d.getLocation());
            }
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("specializations", List.copyOf(specializations));
        result.put("locations", List.copyOf(locations));
        return result;
    }

    @Transactional(readOnly = true)
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + id));
    }

    @Transactional
    public Doctor create(DoctorRequest request) {
        Doctor doctor = new Doctor();
        apply(doctor, request);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor update(Long id, DoctorRequest request) {
        Doctor doctor = findById(id);
        apply(doctor, request);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public void delete(Long id) {
        Doctor doctor = findById(id);
        reviewRepository.deleteByDoctorId(id);   // reviews have a foreign key to the doctor
        doctorRepository.delete(doctor);
        // appointments keep the doctor's name, so patient history is not lost
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews(Long doctorId) {
        findById(doctorId);
        return reviewRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId).stream()
                .map(ReviewResponse::from).toList();
    }

    @Transactional
    public ReviewResponse addReview(Long doctorId, ReviewRequest request, String patientEmail) {
        Doctor doctor = findById(doctorId);
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = new Review();
        review.setDoctor(doctor);
        review.setPatient(patient);
        review.setRating(request.rating());
        review.setComment(request.comment());
        Review saved = reviewRepository.save(review);

        Double average = reviewRepository.averageRating(doctorId);
        if (average != null) {
            doctor.setRating(Math.round(average * 10.0) / 10.0);
            doctorRepository.save(doctor);
        }
        return ReviewResponse.from(saved);
    }

    private void apply(Doctor doctor, DoctorRequest request) {
        doctor.setName(request.name().trim());
        doctor.setSpecialization(request.specialization().trim());
        doctor.setExperience(request.experience());
        doctor.setFees(request.fees());
        doctor.setLocation(request.location().trim());
        doctor.setImage(request.image());
        doctor.setQualification(request.qualification());
        doctor.setAbout(request.about());
        doctor.setLanguages(request.languages());
        doctor.setAvailableDays(request.availableDays());
    }
}
