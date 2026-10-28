package com.followupsaathi.dto;

import jakarta.validation.constraints.Size;

/** Mirrors the shape of User so the client never sees the password hash. */
public record UserDtos() {

    public record Create(
            @Size(max = 120, message = "Name must be at most 120 characters") String name,
            @Size(max = 255, message = "Email must be at most 255 characters") String email,
            @Size(max = 20, message = "Mobile must be at most 20 characters") String mobile) {
    }

    public record Update(
            @Size(max = 120, message = "Name must be at most 120 characters") String name,
            @Size(max = 20, message = "Mobile must be at most 20 characters") String mobile) {
    }

    public record View(
            Long id,
            String name,
            String email,
            String mobile,
            java.time.Instant createdAt) {
    }
}
