package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Business profile, captured during onboarding and editable in Settings. */
public class BusinessDtos {

    public record Upsert(
            @NotBlank(message = "Business name is required")
            @Size(max = 140, message = "Business name must be at most 140 characters")
            String businessName,

            @NotBlank(message = "Business category is required")
            @Size(max = 80, message = "Business category must be at most 80 characters")
            String businessCategory,

            @Size(max = 40, message = "Time zone must be at most 40 characters")
            String timeZone,

            @Size(max = 5, message = "Language code must be at most 5 characters")
            String preferredLanguage,

            List<Integer> workingDays,

            @NotNull(message = "Reminder enabled flag is required")
            Boolean reminderEnabled,

            @NotBlank(message = "Reminder time is required")
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                    message = "Reminder time must be in 24-hour HH:mm format")
            String reminderTime) {
    }

    public record View(
            Long id,
            String businessName,
            String businessCategory,
            String preferredLanguage,
            String timeZone,
            List<Integer> workingDays,
            boolean reminderEnabled,
            String reminderTime,
            String ownerMobile,
            boolean profileComplete,
            java.time.Instant createdAt) {
    }
}
