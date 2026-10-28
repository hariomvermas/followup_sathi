package com.followupsaathi.controller;

import com.followupsaathi.dto.BusinessDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Business profile + reminder settings: read and save in one place. */
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;
    private final CurrentUser currentUser;

    public BusinessController(BusinessService businessService, CurrentUser currentUser) {
        this.businessService = businessService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public BusinessDtos.View get() {
        return businessService.view(currentUser.requireUserId());
    }

    @PutMapping
    public BusinessDtos.View upsert(@Valid @RequestBody BusinessDtos.Upsert request) {
        return businessService.upsert(currentUser.requireUserId(), request);
    }
}
