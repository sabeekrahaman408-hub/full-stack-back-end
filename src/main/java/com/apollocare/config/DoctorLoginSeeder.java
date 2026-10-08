package com.apollocare.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.apollocare.entity.Doctor;
import com.apollocare.entity.Role;
import com.apollocare.entity.User;
import com.apollocare.repository.DoctorRepository;
import com.apollocare.repository.UserRepository;

/**
 * Gives every doctor that has no login yet a ready-to-use account:
 *   email    = <first name without "Dr">@apollocare.com   (Dr CHANDRU -> chandru@apollocare.com)
 *   password = app.seed-doctor-logins.password            (default Doctor@123)
 * Doctors that already have a login are never touched. Turn off with SEED_DOCTOR_LOGINS=false.
 */
@Component
@Order(3)
public class DoctorLoginSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DoctorLoginSeeder.class);

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-doctor-logins.enabled:true}")
    private boolean enabled;

    @Value("${app.seed-doctor-logins.password:Doctor@123}")
    private String password;

    public DoctorLoginSeeder(DoctorRepository doctorRepository, UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        for (Doctor doctor : doctorRepository.findAll()) {
            if (userRepository.findByDoctorId(doctor.getId()).isPresent()) {
                continue;
            }
            String local = doctor.getName().toLowerCase()
                    .replaceFirst("^dr\\.?\\s*", "")
                    .replaceAll("[^a-z0-9]", "");
            if (local.isEmpty()) {
                local = "doctor" + doctor.getId();
            }
            String email = local + "@apollocare.com";
            if (userRepository.existsByEmail(email)) {
                log.warn("Doctor login skipped for {}: email {} is already used by another account", doctor.getName(), email);
                continue;
            }
            User user = new User();
            user.setName(doctor.getName());
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password.trim()));
            user.setRole(Role.DOCTOR);
            user.setDoctorId(doctor.getId());
            userRepository.save(user);
            log.info("Doctor login created: {}", email);
        }
    }
}
