package com.apollocare.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.DoctorAccountRequest;
import com.apollocare.dto.DoctorAccountResponse;
import com.apollocare.dto.UserResponse;
import com.apollocare.entity.Appointment;
import com.apollocare.entity.Doctor;
import com.apollocare.entity.Payment;
import com.apollocare.entity.Role;
import com.apollocare.entity.User;
import com.apollocare.exception.ConflictException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.AppointmentRepository;
import com.apollocare.repository.DoctorRepository;
import com.apollocare.repository.PaymentRepository;
import com.apollocare.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository,
                        PaymentRepository paymentRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Real numbers for the admin dashboard (replaces the hard-coded KPI cards). */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<Appointment> appointments = appointmentRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        Map<String, Long> byStatus = new TreeMap<>();
        Map<String, Long> bySpecialization = new TreeMap<>();
        for (Appointment a : appointments) {
            byStatus.merge(a.getStatus() == null ? "unknown" : a.getStatus(), 1L, Long::sum);
            if (a.getSpecialization() != null) {
                bySpecialization.merge(a.getSpecialization(), 1L, Long::sum);
            }
        }

        // last 6 months, oldest first, including months with zero activity
        YearMonth current = YearMonth.now();
        Map<YearMonth, long[]> months = new LinkedHashMap<>();   // [count, revenue]
        for (int i = 5; i >= 0; i--) {
            months.put(current.minusMonths(i), new long[] {0, 0});
        }
        for (Appointment a : appointments) {
            if (a.getCreatedAt() != null) {
                long[] bucket = months.get(YearMonth.from(a.getCreatedAt()));
                if (bucket != null) {
                    bucket[0]++;
                }
            }
        }
        for (Payment p : payments) {
            if ("Paid".equals(p.getStatus()) && p.getCreatedAt() != null && p.getAmount() != null) {
                long[] bucket = months.get(YearMonth.from(p.getCreatedAt()));
                if (bucket != null) {
                    bucket[1] += Math.round(p.getAmount());
                }
            }
        }
        List<Map<String, Object>> monthly = new ArrayList<>();
        months.forEach((ym, v) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.toString());
            row.put("appointments", v[0]);
            row.put("revenue", v[1]);
            monthly.add(row);
        });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("totalPatients", userRepository.countByRole(Role.PATIENT));
        stats.put("totalAppointments", appointments.size());
        stats.put("pendingAppointments", byStatus.getOrDefault("pending", 0L));
        stats.put("revenue", paymentRepository.totalPaid());
        stats.put("byStatus", byStatus);
        stats.put("bySpecialization", bySpecialization);
        stats.put("monthly", monthly);
        return stats;
    }

    /** Safe, non-sensitive numbers for the public home page. */
    @Transactional(readOnly = true)
    public Map<String, Object> publicStats() {
        double avgRating = doctorRepository.findAll().stream()
                .map(Doctor::getRating).filter(r -> r != null)
                .mapToDouble(Double::doubleValue).average().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("doctors", doctorRepository.count());
        stats.put("patients", userRepository.countByRole(Role.PATIENT));
        stats.put("appointments", appointmentRepository.count());
        stats.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    /** One row per doctor: does this doctor already have a login, and with which email? */
    @Transactional(readOnly = true)
    public List<DoctorAccountResponse> doctorAccounts() {
        Map<Long, User> byDoctor = new java.util.HashMap<>();
        for (User u : userRepository.findByRole(Role.DOCTOR)) {
            if (u.getDoctorId() != null) {
                byDoctor.put(u.getDoctorId(), u);
            }
        }
        return doctorRepository.findAll().stream()
                .map(d -> {
                    User u = byDoctor.get(d.getId());
                    return new DoctorAccountResponse(d.getId(), d.getName(), u == null ? null : u.getEmail(), u != null);
                })
                .toList();
    }

    /** Creates the doctor's login, or changes its email/password if one already exists. */
    @Transactional
    public DoctorAccountResponse saveDoctorAccount(Long doctorId, DoctorAccountRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));
        String email = request.email().trim().toLowerCase();

        User existing = userRepository.findByDoctorId(doctorId).orElse(null);
        User other = userRepository.findByEmail(email).orElse(null);
        if (other != null && (existing == null || !other.getId().equals(existing.getId()))) {
            throw new ConflictException("That email is already used by another account");
        }

        User user = existing != null ? existing : new User();
        user.setName(doctor.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password().trim()));
        user.setRole(Role.DOCTOR);
        user.setDoctorId(doctorId);
        userRepository.save(user);
        return new DoctorAccountResponse(doctorId, doctor.getName(), email, true);
    }
}
