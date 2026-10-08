package com.apollocare.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.PaymentResponse;
import com.apollocare.entity.Appointment;
import com.apollocare.entity.Payment;
import com.apollocare.exception.BadRequestException;
import com.apollocare.exception.ResourceNotFoundException;
import com.apollocare.repository.AppointmentRepository;
import com.apollocare.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Set<String> STATUSES = Set.of("Pending", "Paid", "Failed", "Refunded");

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    public PaymentService(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Records the payment for a new appointment.
     * SECURITY: the full card number and CVV are never stored - only the last 4 digits.
     * (The frontend payment form is a demo; use a real gateway such as Razorpay/Stripe for live money.)
     */
    @Transactional
    public Payment recordForAppointment(Appointment appointment, Map<String, String> details) {
        Payment payment = new Payment();
        payment.setAppointmentId(appointment.getId());
        payment.setPatientEmail(appointment.getEmail());
        payment.setAmount(appointment.getConsultationFee());
        payment.setMethod(appointment.getPaymentMethod());
        payment.setStatus(appointment.getPaymentStatus());

        if (details != null) {
            String cardNumber = details.get("cardNumber");
            if (cardNumber != null) {
                String digits = cardNumber.replaceAll("\\D", "");
                if (digits.length() >= 4) {
                    payment.setCardLast4(digits.substring(digits.length() - 4));
                }
                payment.setCardHolder(details.get("cardName"));
            }
            payment.setUpiId(details.get("upiId"));
            payment.setTransactionId(details.get("transactionId"));
        }
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream().map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByAppointment(Long appointmentId, String email, boolean admin) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + appointmentId));
        if (!admin && !appointment.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You can only view your own payments");
        }
        Payment payment = paymentRepository.findFirstByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for this appointment"));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse updateStatus(Long paymentId, String status) {
        String normalized = normalize(status);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + paymentId));
        payment.setStatus(normalized);
        Payment saved = paymentRepository.save(payment);

        // keep the appointment's paymentStatus in sync
        appointmentRepository.findById(saved.getAppointmentId()).ifPresent(a -> {
            a.setPaymentStatus(normalized);
            appointmentRepository.save(a);
        });
        return PaymentResponse.from(saved);
    }

    private String normalize(String status) {
        for (String allowed : STATUSES) {
            if (allowed.equalsIgnoreCase(status.trim())) {
                return allowed;
            }
        }
        throw new BadRequestException("Payment status must be one of " + STATUSES);
    }
}
