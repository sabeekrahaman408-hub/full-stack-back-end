package com.apollocare.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.apollocare.entity.Role;
import com.apollocare.entity.User;
import com.apollocare.repository.UserRepository;

/**
 * Makes sure the configured administrator account exists and really is an ADMIN on every start.
 *
 * - no user with that email  -> creates the admin
 * - user exists but is a PATIENT (e.g. it was registered from the Register page earlier) -> promoted to ADMIN
 * - app.admin.reset-password=true -> password is reset to app.admin.password (use if you forgot it)
 */
@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.reset-password:false}")
    private boolean resetPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String email = adminEmail.trim().toLowerCase();
        User admin = userRepository.findByEmail(email).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setName(adminName);
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(adminPassword.trim()));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Admin account created: {}", email);
            return;
        }

        boolean changed = false;
        if (admin.getRole() != Role.ADMIN) {
            admin.setRole(Role.ADMIN);
            changed = true;
            log.info("Existing account {} promoted to ADMIN", email);
        }
        if (resetPassword) {
            admin.setPassword(passwordEncoder.encode(adminPassword.trim()));
            changed = true;
            log.info("Admin password reset for {}", email);
        }
        if (changed) {
            userRepository.save(admin);
        }
    }
}
