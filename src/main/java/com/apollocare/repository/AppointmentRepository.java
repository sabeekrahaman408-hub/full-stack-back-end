package com.apollocare.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apollocare.entity.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByOrderByCreatedAtDesc();

    List<Appointment> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    long countByStatus(String status);

    List<Appointment> findByDoctorIdOrderByAppointmentDateAscAppointmentTimeAsc(Long doctorId);

    @Query("select a.appointmentTime from Appointment a "
            + "where lower(a.doctorName) = lower(:doctor) and a.appointmentDate = :date "
            + "and a.status not in ('cancelled', 'rejected')")
    List<String> bookedTimes(@Param("doctor") String doctor, @Param("date") LocalDate date);

    @Query("select count(a) from Appointment a "
            + "where lower(a.doctorName) = lower(:doctor) "
            + "and a.appointmentDate = :date and a.appointmentTime = :time "
            + "and a.status not in ('cancelled', 'rejected')")
    long countActiveInSlot(@Param("doctor") String doctor,
                      @Param("date") LocalDate date,
                      @Param("time") String time);
}
