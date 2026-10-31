package com.fitnessaddicted.gym.admin.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.admin.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public DashboardResponse get() {
        return dashboard.summary();
    }
}