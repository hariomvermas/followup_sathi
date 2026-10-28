package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Follow-ups are scheduled per day (the business works from a daily reminder
 * list), so they carry a date and a reason — no per-item clock time.
 */
public class FollowUpDtos {

    public record Create(
            @NotBlank(message = "Date is required")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be YYYY-MM-DD")
            String date,

            @Size(max = 300, message = "Reason must be at most 300 characters")
            String reason) {
    }

    /** Both fields optional; omitted fields keep their current value. */
    public record Update(
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be YYYY-MM-DD")
            String date,

            @Size(max = 300, message = "Reason must be at most 300 characters")
            String reason) {
    }

    public record View(
            Long id,
            Long enquiryId,
            String customerName,
            String customerPhone,
            String serviceName,
            String date,
            String reason,
            String status,
            String outcomeType,
            String outcome,
            java.time.Instant completedAt,
            java.time.Instant createdAt) {
    }

    public record Complete(
            @NotBlank(message = "Outcome is required")
            @Pattern(regexp = "INTERESTED|NOT_INTERESTED|NO_RESPONSE|POSTPONED",
                    message = "Unknown outcome")
            String outcomeType,

            @Size(max = 2000, message = "Notes must be at most 2000 characters")
            String notes) {
    }

    /** One row of a business-wide pending follow-up list. */
    public record PendingItem(
            Long followUpId,
            Long enquiryId,
            String customerName,
            String customerPhone,
            String serviceName,
            String date,
            String reason,
            boolean overdue) {
    }

    /** Bucketed pending list: today, later, overdue. */
    public record ListResponse(
            long todayCount,
            long upcomingCount,
            long overdueCount,
            List<PendingItem> items) {
    }
}
