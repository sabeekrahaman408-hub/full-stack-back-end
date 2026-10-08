package com.apollocare.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apollocare.dto.SlotResponse;
import com.apollocare.entity.Doctor;
import com.apollocare.exception.BadRequestException;
import com.apollocare.repository.AppointmentRepository;

/**
 * Single source of truth for "when can a patient book?".
 * - dates are checked in the configured time zone (app.timezone), not the server's default zone
 * - no past dates, no past time slots today, no Sundays, nothing beyond app.booking.max-days-ahead
 */
@Service
public class SlotService {

    private static final DateTimeFormatter CLOCK = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("hh:mm a").toFormatter(Locale.ENGLISH);

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final ZoneId zone;
    private final int maxDaysAhead;

    public SlotService(AppointmentRepository appointmentRepository,
                       DoctorService doctorService,
                       @Value("${app.timezone:Asia/Kolkata}") String timezone,
                       @Value("${app.booking.max-days-ahead:60}") int maxDaysAhead) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.zone = ZoneId.of(timezone);
        this.maxDaysAhead = maxDaysAhead;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    /** 09:00-13:00 and 16:00-19:00, every 30 minutes. */
    public List<String> allSlots() {
        List<String> slots = new ArrayList<>();
        addRange(slots, LocalTime.of(9, 0), LocalTime.of(13, 0));
        addRange(slots, LocalTime.of(16, 0), LocalTime.of(19, 0));
        return slots;
    }

    private void addRange(List<String> slots, LocalTime from, LocalTime to) {
        for (LocalTime t = from; t.isBefore(to); t = t.plusMinutes(30)) {
            slots.add(CLOCK.format(t).toLowerCase(Locale.ENGLISH) + " - "
                    + CLOCK.format(t.plusMinutes(30)).toLowerCase(Locale.ENGLISH));
        }
    }

    /** Throws BadRequestException with a clear message when this date/time cannot be booked. */
    public void validate(LocalDate date, String slot) {
        validateDate(date);
        if (!allSlots().contains(slot)) {
            throw new BadRequestException("Please choose one of the available time slots");
        }
        if (date.equals(today()) && isPast(slot)) {
            throw new BadRequestException("This time slot has already passed. Please choose a later slot.");
        }
    }

    public void validateDate(LocalDate date) {
        LocalDate today = today();
        if (date.isBefore(today)) {
            throw new BadRequestException("You cannot book an appointment on a past date");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Sunday is not available for booking");
        }
        if (date.isAfter(today.plusDays(maxDaysAhead))) {
            throw new BadRequestException("You can only book up to " + maxDaysAhead + " days ahead");
        }
    }

    /** True when the appointment's start time is already in the past (in the configured time zone). */
    public boolean hasStarted(LocalDate date, String slot) {
        try {
            LocalTime start = LocalTime.parse(slot.split(" - ")[0].trim(), CLOCK);
            return LocalDateTime.of(date, start).isBefore(LocalDateTime.now(zone));
        } catch (Exception e) {
            return date.isBefore(today());
        }
    }

    private boolean isPast(String slot) {
        LocalTime start = LocalTime.parse(slot.split(" - ")[0].trim(), CLOCK);
        return LocalDateTime.of(today(), start).isBefore(LocalDateTime.now(zone));
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> slotsFor(Long doctorId, LocalDate date) {
        Doctor doctor = doctorService.findById(doctorId);
        validateDate(date);
        Set<String> booked = new HashSet<>(appointmentRepository.bookedTimes(doctor.getName(), date));
        boolean today = date.equals(today());

        List<SlotResponse> result = new ArrayList<>();
        for (String slot : allSlots()) {
            if (booked.contains(slot)) {
                result.add(new SlotResponse(slot, false, "Booked"));
            } else if (today && isPast(slot)) {
                result.add(new SlotResponse(slot, false, "Passed"));
            } else {
                result.add(new SlotResponse(slot, true, null));
            }
        }
        return result;
    }
}
