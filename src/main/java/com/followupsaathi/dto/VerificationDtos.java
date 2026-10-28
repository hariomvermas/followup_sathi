package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mobile verification: sending the code and confirming it. */
public class VerificationDtos {

    public record SendRequest(
            @NotBlank(message = "Mobile number is required")
            @Pattern(regexp = "^[0-9+\\-\\s]{10,15}$",
                    message = "Enter a valid mobile number")
            String phoneNumber) {
    }

    public record SendResponse(
            String maskedPhone,
            String code,
            String whatsappLink,
            String smsLink,
            java.time.Instant expiresAt) {
    }

    public record VerifyRequest(
            @NotBlank(message = "Mobile number is required")
            String phoneNumber,

            @NotBlank(message = "Code is required")
            @Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits")
            String code) {
    }

    public record VerifyResponse(boolean verified, String message) {
    }
}
