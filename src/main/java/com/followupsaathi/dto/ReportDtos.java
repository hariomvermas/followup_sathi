package com.followupsaathi.dto;

import java.util.List;

/**
 * Aggregate report numbers. "This month" is computed in the business's own
 * time zone; rates are 0-100 with one decimal and are safe on zero totals.
 */
public class ReportDtos {

    public record View(
            String range,
            String fromDate,
            String toDate,
            long enquiriesCreated,
            long enquiriesConverted,
            long followUpsCompleted,
            long followUpsScheduled,
            long pendingFollowUps,
            long activeEnquiries,
            long archivedEnquiries,
            Double conversionRatePercent,
            List<StatusCount> statusCounts) {
    }

    /** One bucket of the enquiry status breakdown (six statuses). */
    public record StatusCount(
            String status,
            long count) {
    }
}
