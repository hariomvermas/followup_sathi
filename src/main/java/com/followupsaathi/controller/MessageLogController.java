package com.followupsaathi.controller;

import com.followupsaathi.dto.MessageLogDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.MessageLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** History of messages the owner sent by opening the messaging app. */
@RestController
@RequestMapping("/api/enquiries/{enquiryId}/messages")
public class MessageLogController {

    private final MessageLogService messageLogService;
    private final CurrentUser currentUser;

    public MessageLogController(MessageLogService messageLogService,
            CurrentUser currentUser) {
        this.messageLogService = messageLogService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageLogDtos.View record(@PathVariable Long enquiryId,
            @Valid @RequestBody MessageLogDtos.Create request) {
        // The path variable is authoritative; the body's enquiryId is ignored.
        return messageLogService.record(currentUser.requireUserId(),
                new MessageLogDtos.Create(enquiryId, request.messageBody(),
                        request.channel()));
    }

    @GetMapping
    public Page<MessageLogDtos.View> history(@PathVariable Long enquiryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return messageLogService.history(currentUser.requireUserId(), enquiryId,
                page, size);
    }
}
