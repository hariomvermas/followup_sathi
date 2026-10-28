package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Messages the owner wrote themselves: history only, nothing auto-sent. */
public class MessageLogDtos {

    public record Create(
            @NotNull(message = "Enquiry is required")
            Long enquiryId,

            @NotBlank(message = "Message body is required")
            @Size(max = 2000, message = "Message must be at most 2000 characters")
            String messageBody,

            @NotBlank(message = "Channel is required")
            @Pattern(regexp = "WHATSAPP|SMS", message = "Channel must be WHATSAPP or SMS")
            String channel) {
    }

    public record View(
            Long id,
            Long enquiryId,
            String customerName,
            String channel,
            String messageBody,
            java.time.Instant sentAt) {
    }
}
