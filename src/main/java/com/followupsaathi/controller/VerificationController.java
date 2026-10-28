package com.followupsaathi.controller;

import com.followupsaathi.dto.VerificationDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobile verification without an SMS provider: the backend returns the code
 * plus ready-to-open WhatsApp/SMS links; the owner sends it to themselves.
 */
@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;
    private final CurrentUser currentUser;

    public VerificationController(VerificationService verificationService,
            CurrentUser currentUser) {
        this.verificationService = verificationService;
        this.currentUser = currentUser;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public VerificationDtos.SendResponse send(
            @Valid @RequestBody VerificationDtos.SendRequest request) {
        return verificationService.send(currentUser.requireUserId(), request.phoneNumber());
    }

    @PostMapping("/verify")
    public VerificationDtos.VerifyResponse verify(
            @Valid @RequestBody VerificationDtos.VerifyRequest request) {
        return verificationService.verify(currentUser.requireUserId(),
                request.phoneNumber(), request.code());
    }
}
