package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Requests and responses for registration, login and session info. */
public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 120, message = "Name must be at most 120 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Size(max = 255, message = "Email must be at most 255 characters")
            @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                    message = "Enter a valid email address")
            String email,

            @Size(max = 20, message = "Mobile must be at most 20 characters")
            @Pattern(regexp = "^[0-9+\\-\\s]{0,20}$",
                    message = "Mobile can contain only digits, spaces, + and -")
            String mobile,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
            String password,

            @NotBlank(message = "Confirm password is required")
            String confirmPassword) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Size(max = 255, message = "Email must be at most 255 characters")
            String email,

            @NotBlank(message = "Password is required")
            @Size(max = 72, message = "Password must be at most 72 characters")
            String password) {
    }

    public record AuthResponse(
            Long userId,
            String name,
            String email,
            boolean hasBusiness,
            boolean businessProfileComplete) {
    }
}
