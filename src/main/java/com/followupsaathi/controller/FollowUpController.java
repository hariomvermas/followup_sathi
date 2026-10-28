package com.followupsaathi.controller;

import com.followupsaathi.dto.FollowUpDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.FollowUpService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Follow-up lifecycle endpoints, all scoped by the session's business. */
@RestController
@RequestMapping("/api/enquiries/{enquiryId}/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;
    private final CurrentUser currentUser;

    public FollowUpController(FollowUpService followUpService, CurrentUser currentUser) {
        this.followUpService = followUpService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpDtos.View schedule(@PathVariable Long enquiryId,
            @Valid @RequestBody FollowUpDtos.Create request) {
        return followUpService.schedule(currentUser.requireUserId(), enquiryId, request);
    }

    @GetMapping
    public List<FollowUpDtos.View> list(@PathVariable Long enquiryId) {
        return followUpService.listForEnquiry(currentUser.requireUserId(), enquiryId);
    }

    @PatchMapping("/{followUpId}/reschedule")
    public FollowUpDtos.View reschedule(@PathVariable Long enquiryId,
            @PathVariable Long followUpId,
            @Valid @RequestBody FollowUpDtos.Update request) {
        return followUpService.reschedule(currentUser.requireUserId(), followUpId, request);
    }

    @PatchMapping("/{followUpId}/complete")
    public FollowUpDtos.View complete(@PathVariable Long enquiryId,
            @PathVariable Long followUpId,
            @Valid @RequestBody FollowUpDtos.Complete request) {
        return followUpService.complete(currentUser.requireUserId(), followUpId, request);
    }
}
