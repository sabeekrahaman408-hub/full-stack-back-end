package com.apollocare.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.apollocare.dto.LoginRequest;
import com.apollocare.dto.LoginResponse;
import com.apollocare.dto.RegisterRequest;
import com.apollocare.entity.Role;
import com.apollocare.entity.User;
import com.apollocare.exception.ConflictException;
import com.apollocare.repository.UserRepository;
import com.apollocare.security.CustomUserDetailsService;
import com.apollocare.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.allow-admin-registration:false}")
    private boolean allowAdminRegistration;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       CustomUserDetailsService userDetailsService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.loginAttemptService = loginAttemptService;
    }

    public Map<String, String> register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered. Please login.");
        }

        Role role = Role.PATIENT;
        if ("admin".equalsIgnoreCase(request.role())) {
            if (!allowAdminRegistration) {
                throw new AccessDeniedException(
                        "Admin registration is disabled. Ask an existing admin for access.");
            }
            role = Role.ADMIN;
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        userRepository.save(user);

        return Map.of("message", "Registration successful");
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        loginAttemptService.checkAllowed(email);
        try {
            // throws BadCredentialsException when email/password is wrong
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(email);
            throw ex;
        }
        loginAttemptService.reset(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if ("admin".equalsIgnoreCase(request.role()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("This account does not have admin access");
        }
        if ("doctor".equalsIgnoreCase(request.role()) && user.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("This account is not a doctor account");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(email));
        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name().toLowerCase(), user.getDoctorId());
    }
}
