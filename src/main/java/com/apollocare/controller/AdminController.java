package com.apollocare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.dto.DoctorAccountRequest;
import com.apollocare.dto.DoctorAccountResponse;
import com.apollocare.dto.UserResponse;

import jakarta.validation.Valid;
import com.apollocare.service.AdminService;

/** Everything under /admin/** requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.stats();
    }

    /** Which doctors already have a login. */
    @GetMapping("/doctor-accounts")
    public List<DoctorAccountResponse> doctorAccounts() {
        return adminService.doctorAccounts();
    }

    /** Create (or update) the login a doctor uses for the Doctor Panel. */
    @PutMapping("/doctors/{doctorId}/account")
    public DoctorAccountResponse saveDoctorAccount(@PathVariable Long doctorId,
                                                   @Valid @RequestBody DoctorAccountRequest request) {
        return adminService.saveDoctorAccount(doctorId, request);
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminService.users();
    }
}
