package com.followupsaathi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Template CRUD, preview rendering and share payloads. */
public class TemplateDtos {

    public record Create(
            @NotBlank(message = "Template name is required")
            @Size(max = 80, message = "Template name must be at most 80 characters")
            String templateName,

            @NotBlank(message = "Message body is required")
            @Size(max = 2000, message = "Message body must be at most 2000 characters")
            String messageBody) {
    }

    public record Update(
            @Size(max = 80, message = "Template name must be at most 80 characters")
            String templateName,

            @Size(max = 2000, message = "Message body must be at most 2000 characters")
            String messageBody) {
    }

    public record View(
            Long id,
            String templateName,
            String messageBody,
            boolean isDefault,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {
    }

    /** Preview uses sample data; an empty list means all placeholders resolved. */
    public record Preview(
            String rendered,
            java.util.List<String> missingPlaceholders) {
    }

    public record Share(
            Long enquiryId,
            String channel,
            String renderedMessage,
            String deepLink) {
    }
}
