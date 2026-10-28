package com.followupsaathi.controller;

import com.followupsaathi.dto.AccountDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account management for the settings screen (spec section 4.12). */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    public AccountController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @PostMapping("/password")
    public Map<String, String> changePassword(
            @Valid @RequestBody AccountDtos.ChangePasswordRequest request) {
        accountService.changePassword(currentUser.requireUserId(), request);
        return Map.of("message", "Password changed");
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @Valid @RequestBody AccountDtos.DeleteAccountRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        accountService.deleteAccount(currentUser.requireUserId(), request,
                httpRequest, httpResponse);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}
