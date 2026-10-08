package com.apollocare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.apollocare.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    @Query("select coalesce(sum(p.amount), 0.0) from Payment p where p.status = 'Paid'")
    Double totalPaid();
}
