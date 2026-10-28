package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Enquiry create/update payloads, list responses and history entries. */
public class EnquiryDtos {

    public record Create(
            @NotBlank(message = "Customer name is required")
            @Size(max = 120, message = "Customer name must be at most 120 characters")
            String customerName,

            @Size(max = 20, message = "Phone number must be at most 20 characters")
            @Pattern(regexp = "^[0-9+\\-\\s]*$", message = "Phone number can contain only digits, spaces, + and -")
            String phoneNumber,

            @Size(max = 180, message = "Email must be at most 180 characters")
            @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address")
            String email,

            @Size(max = 140, message = "Service name must be at most 140 characters")
            String serviceName,

            @NotBlank(message = "Source is required")
            @Pattern(regexp = "PHONE_CALL|WALK_IN|WHATSAPP|INSTAGRAM|REFERRAL|OTHER",
                    message = "Unknown enquiry source")
            String source,

            @NotBlank(message = "Priority is required")
            @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Unknown priority")
            String priority,

            @Size(max = 2000, message = "Notes must be at most 2000 characters")
            String notes) {
    }

    /** All fields optional: only provided (non-null) values are changed. */
    public record Update(
            @Size(max = 120, message = "Customer name must be at most 120 characters")
            String customerName,

            @Size(max = 20, message = "Phone number must be at most 20 characters")
            @Pattern(regexp = "^[0-9+\\-\\s]*$", message = "Phone number can contain only digits, spaces, + and -")
            String phoneNumber,

            @Size(max = 180, message = "Email must be at most 180 characters")
            @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address")
            String email,

            @Size(max = 140, message = "Service name must be at most 140 characters")
            String serviceName,

            @Pattern(regexp = "PHONE_CALL|WALK_IN|WHATSAPP|INSTAGRAM|REFERRAL|OTHER",
                    message = "Unknown enquiry source")
            String source,

            @Pattern(regexp = "NEW|CONTACTED|INTERESTED|FOLLOW_UP_REQUIRED|CONVERTED|NOT_INTERESTED", message = "Unknown status")
            String status,

            @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Unknown priority")
            String priority,

            @Size(max = 2000, message = "Notes must be at most 2000 characters")
            String notes) {
    }

    public record View(
            Long id,
            String customerName,
            String phoneNumber,
            String email,
            String serviceName,
            String source,
            String status,
            String priority,
            java.time.LocalDate nextFollowUpDate,
            String notes,
            boolean archived,
            java.time.Instant convertedAt,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            Integer pendingFollowUps) {
    }

    public record ListItem(
            Long id,
            String customerName,
            String phoneNumber,
            String serviceName,
            String status,
            String priority,
            java.time.LocalDate nextFollowUpDate,
            boolean hasPendingFollowUp,
            java.time.Instant createdAt) {
    }

    public record ListResponse(
            java.util.List<ListItem> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record Activity(
            Long id,
            String eventType,
            String description,
            String actorName,
            java.time.Instant createdAt) {
    }
}
