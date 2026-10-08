package com.apollocare.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.AppointmentResponse;
import com.apollocare.dto.ConsultationRequest;
import com.apollocare.dto.MedicalRecordResponse;
import com.apollocare.entity.Appointment;
import com.apollocare.entity.Doctor;
import com.apollocare.entity.MedicalRecord;
import com.apollocare.entity.User;
import com.apollocare.exception.BadRequestException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.AppointmentRepository;
import com.apollocare.repository.DoctorRepository;
import com.apollocare.repository.MedicalRecordRepository;
import com.apollocare.repository.UserRepository;

/** Everything a logged-in doctor can do - always restricted to that doctor's own appointments. */
@Service
public class DoctorPortalService {

    private static final Set<String> DOCTOR_STATUSES = Set.of("approved", "rejected", "completed", "no-show", "cancelled");

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentService appointmentService;
    private final SlotService slotService;

    public DoctorPortalService(UserRepository userRepository,
                               DoctorRepository doctorRepository,
                               AppointmentRepository appointmentRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               AppointmentService appointmentService,
                               SlotService slotService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.appointmentService = appointmentService;
        this.slotService = slotService;
    }

    private Doctor doctorOf(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getDoctorId() == null) {
            throw new AccessDeniedException("This account is not linked to a doctor profile. Ask the admin to link it.");
        }
        return doctorRepository.findById(user.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Your doctor profile no longer exists"));
    }

    private Appointment owned(String email, Long appointmentId) {
        Doctor doctor = doctorOf(email);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));
        boolean mine = doctor.getId().equals(appointment.getDoctorId())
                || (appointment.getDoctorId() == null && doctor.getName().equalsIgnoreCase(appointment.getDoctorName()));
        if (!mine) {
            throw new AccessDeniedException("This appointment belongs to another doctor");
        }
        return appointment;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview(String email) {
        Doctor doctor = doctorOf(email);
        List<Appointment> mine = appointmentRepository.findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId());
        var today = slotService.today();

        long pending = mine.stream().filter(a -> "pending".equals(a.getStatus())).count();
        long todays = mine.stream().filter(a -> today.equals(a.getAppointmentDate())
                && ("approved".equals(a.getStatus()) || "scheduled".equals(a.getStatus()))).count();
        long completed = mine.stream().filter(a -> "completed".equals(a.getStatus())).count();
        long patients = mine.stream().map(a -> a.getEmail().toLowerCase()).distinct().count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("doctor", doctor);
        out.put("pending", pending);
        out.put("today", todays);
        out.put("completed", completed);
        out.put("totalAppointments", mine.size());
        out.put("totalPatients", patients);
        return out;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> appointments(String email) {
        Doctor doctor = doctorOf(email);
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(doctor.getId())
                .stream().map(AppointmentResponse::from).toList();
    }

    /** Accept / reject / complete / no-show. */
    @Transactional
    public AppointmentResponse updateStatus(String email, Long id, String status, String note) {
        Appointment appointment = owned(email, id);
        String target = status.trim().toLowerCase();
        if (!DOCTOR_STATUSES.contains(target)) {
            throw new BadRequestException("Doctors can set: " + DOCTOR_STATUSES);
        }
        String current = appointment.getStatus();
        boolean open = "approved".equals(current) || "scheduled".equals(current);

        switch (target) {
            case "approved", "rejected" -> {
                if (!"pending".equals(current)) {
                    throw new BadRequestException("Only pending requests can be accepted or rejected (this one is " + current + ")");
                }
            }
            case "completed", "no-show", "cancelled" -> {
                if (!open) {
                    throw new BadRequestException("Accept the request first - this appointment is " + current);
                }
            }
            default -> { }
        }
        return appointmentService.updateStatus(id, target, note);
    }

    /** Writes diagnosis/prescription to the patient's medical record and (optionally) completes the visit. */
    @Transactional
    public MedicalRecordResponse addConsultation(String email, Long appointmentId, ConsultationRequest request) {
        Doctor doctor = doctorOf(email);
        Appointment appointment = owned(email, appointmentId);

        User patient = userRepository.findByEmail(appointment.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found for " + appointment.getEmail()));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setAppointmentId(appointment.getId());
        record.setDoctorName(doctor.getName());
        record.setSource("doctor");
        record.setCategory("Consultation");
        record.setRecordDate(slotService.today());
        record.setDiagnosis(request.diagnosis().trim());
        record.setPrescription(request.prescription());
        record.setNotes(request.notes());
        MedicalRecord saved = medicalRecordRepository.save(record);

        boolean complete = request.markCompleted() == null || request.markCompleted();
        String current = appointment.getStatus();
        if (complete && ("approved".equals(current) || "scheduled".equals(current))) {
            appointmentService.updateStatus(appointmentId, "completed", null);
        }
        return MedicalRecordResponse.from(saved);
    }
}
