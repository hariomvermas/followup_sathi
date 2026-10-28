package com.followupsaathi.controller;

import com.followupsaathi.dto.DashboardDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One endpoint feeding the whole dashboard screen. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardDtos.View view() {
        return dashboardService.view(currentUser.requireUserId());
    }
}
