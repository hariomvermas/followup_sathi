package com.followupsaathi.dto;

/**
 * Dashboard numbers. Every count is scoped to one business and computed in
 * the business's time zone, never the server's.
 */
public class DashboardDtos {

    /** One line in the today/overdue follow-up list. */
    public record FollowUpItem(
            Long followUpId,
            Long enquiryId,
            String customerName,
            String customerPhone,
            String serviceName,
            String date,
            String reason,
            boolean overdue) {
    }

    public record View(
            long totalEnquiries,
            long newEnquiries,
            long contacted,
            long interested,
            long converted,
            long followUpRequired,
            long dueToday,
            long overdue,
            long completedThisWeek,
            java.util.List<FollowUpItem> todayItems) {
    }
}
