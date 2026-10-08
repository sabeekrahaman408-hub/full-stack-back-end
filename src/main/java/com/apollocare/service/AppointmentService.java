package com.apollocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.AppointmentRequest;
import com.apollocare.dto.AppointmentResponse;
import com.apollocare.entity.Appointment;
import com.apollocare.entity.Doctor;
import com.apollocare.exception.BadRequestException;
import com.apollocare.exception.ConflictException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.AppointmentRepository;
import com.apollocare.repository.DoctorRepository;

@Service
public class AppointmentService {

    private static final Set<String> STATUSES =
            Set.of("pending", "approved", "scheduled", "completed", "cancelled", "rejected", "no-show");

    private static final Set<String> TERMINAL = Set.of("completed", "cancelled", "rejected", "no-show");

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PaymentService paymentService;
    private final SlotService slotService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PaymentService paymentService,
                              SlotService slotService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.paymentService = paymentService;
        this.slotService = slotService;
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, String userEmail) {
        slotService.validate(request.date(), request.time().trim());

        String doctorName = request.doctor().trim();
        Doctor doctor = doctorRepository.findFirstByNameIgnoreCase(doctorName)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorName));

        String time = request.time().trim();
        if (appointmentRepository.countActiveInSlot(doctor.getName(), request.date(), time) > 0) {
            throw new ConflictException(
                    "This slot is already booked for " + doctor.getName() + ". Please choose another time.");
        }

        boolean cash = "cash".equalsIgnoreCase(request.paymentMethod().trim());

        Appointment appointment = new Appointment();
        appointment.setDoctorId(doctor.getId());
        appointment.setDoctorName(doctor.getName());
        appointment.setSpecialization(doctor.getSpecialization());
        appointment.setAppointmentDate(request.date());
        appointment.setAppointmentTime(time);
        appointment.setPatientName(request.patientName().trim());
        appointment.setAge(request.age());
        appointment.setContact(request.contact().trim());
        appointment.setProblem(request.problem());
        appointment.setEmail(userEmail);                        // always the logged-in user
        appointment.setRequestedBy(userEmail);
        // fee comes from the doctor record, not from the browser
        appointment.setConsultationFee(doctor.getFees() != null ? doctor.getFees() : request.consultationFee());
        appointment.setPaymentMethod(request.paymentMethod().trim());
        appointment.setPaymentStatus(cash ? "Pending" : "Paid");
        appointment.setStatus("pending");                       // waits for admin approval
        appointment.setType(request.type() != null ? request.type() : "in-person");

        Appointment saved = appointmentRepository.save(appointment);
        paymentService.recordForAppointment(saved, request.paymentDetails());
        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(String userEmail, boolean admin, String emailFilter) {
        List<Appointment> result;
        if (admin) {
            result = (emailFilter == null || emailFilter.isBlank())
                    ? appointmentRepository.findAllByOrderByCreatedAtDesc()
                    : appointmentRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(emailFilter.trim());
        } else {
            // patients only ever see their own appointments, whatever ?email= they send
            result = appointmentRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
        }
        return result.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id, String userEmail, boolean admin) {
        return AppointmentResponse.from(loadOwned(id, userEmail, admin));
    }

    @Transactional
    public void delete(Long id, String userEmail, boolean admin) {
        appointmentRepository.delete(loadOwned(id, userEmail, admin));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String status, String note) {
        String normalized = status.trim().toLowerCase();
        if (!STATUSES.contains(normalized)) {
            throw new BadRequestException("Status must be one of " + STATUSES);
        }
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));
        if (TERMINAL.contains(appointment.getStatus()) && !appointment.getStatus().equals(normalized)) {
            throw new BadRequestException(
                    "This appointment is already " + appointment.getStatus() + " and can no longer be changed");
        }
        appointment.setStatus(normalized);
        if (note != null && !note.isBlank()) {
            appointment.setAdminNote(note.trim());
        }
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    /** Patient (own appointment) or admin cancels; frees the slot for other patients. */
    @Transactional
    public AppointmentResponse cancel(Long id, String userEmail, boolean admin, String reason) {
        Appointment appointment = loadOwned(id, userEmail, admin);
        if (TERMINAL.contains(appointment.getStatus())) {
            throw new BadRequestException("This appointment is already " + appointment.getStatus());
        }
        if (!admin && slotService.hasStarted(appointment.getAppointmentDate(), appointment.getAppointmentTime())) {
            throw new BadRequestException("This appointment time has already passed and can no longer be cancelled");
        }
        appointment.setStatus("cancelled");
        if (reason != null && !reason.isBlank()) {
            appointment.setCancelReason(reason.trim());
        }
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private Appointment loadOwned(Long id, String userEmail, boolean admin) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));
        if (!admin && !appointment.getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You can only access your own appointments");
        }
        return appointment;
    }
}
