package com.apollocare.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Older databases created users.role as a native MySQL ENUM('ADMIN','PATIENT'), which cannot store the new
 * DOCTOR role. This converts that one column to VARCHAR(20) in place - all rows and data are kept.
 * Safe to run on every start (does nothing once the column is already VARCHAR).
 */
@Component
@Order(1)
public class SchemaUpgrade implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final JdbcTemplate jdbc;

    public SchemaUpgrade(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            String type = jdbc.queryForObject(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'",
                    String.class);
            if ("enum".equalsIgnoreCase(type)) {
                jdbc.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL");
                log.info("users.role converted from ENUM to VARCHAR(20) to support the DOCTOR role");
            }
        } catch (Exception e) {
            log.warn("Could not check/convert users.role column: {}", e.getMessage());
        }
    }
}
