package com.followupsaathi.controller;

import com.followupsaathi.dto.FollowUpDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.FollowUpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business-wide pending follow-up lists for the dedicated follow-ups screen
 * (spec section 8): overdue and today first, then upcoming. Per-enquiry
 * scheduling and completion stay on /api/enquiries/{id}/follow-ups.
 */
@RestController
@RequestMapping("/api/follow-ups")
public class GlobalFollowUpController {

    private final FollowUpService followUpService;
    private final CurrentUser currentUser;

    public GlobalFollowUpController(FollowUpService followUpService,
            CurrentUser currentUser) {
        this.followUpService = followUpService;
        this.currentUser = currentUser;
    }

    /** bucket: ALL (default), TODAY, UPCOMING or OVERDUE. */
    @GetMapping
    public FollowUpDtos.ListResponse list(
            @RequestParam(name = "bucket", required = false) String bucket) {
        return followUpService.listPending(currentUser.requireUserId(), bucket);
    }
}
