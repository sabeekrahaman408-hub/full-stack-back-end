package com.apollocare.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apollocare.service.AdminService;

/** No login needed - powers the live numbers on the home page. */
@RestController
@RequestMapping("/public")
public class PublicController {

    private final AdminService adminService;

    public PublicController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.publicStats();
    }
}
