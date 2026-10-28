package com.followupsaathi.dto;

/** Account-management payloads: password change and account deletion. */
public class AccountDtos {

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank(message = "Current password is required")
            String currentPassword,

            @jakarta.validation.constraints.NotBlank(message = "New password is required")
            @jakarta.validation.constraints.Size(min = 8, max = 72,
                    message = "New password must be 8-72 characters")
            String newPassword) {
    }

    public record DeleteAccountRequest(
            @jakarta.validation.constraints.NotBlank(message = "Password is required")
            String password,

            @jakarta.validation.constraints.NotBlank(
                    message = "Type DELETE to confirm account deletion")
            @jakarta.validation.constraints.Pattern(regexp = "DELETE",
                    message = "Type DELETE to confirm account deletion")
            String confirmText) {
    }
}
