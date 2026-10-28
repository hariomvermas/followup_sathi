package com.followupsaathi.service;

import com.followupsaathi.dto.ReportDtos;
import com.followupsaathi.entity.EnquiryStatus;
import com.followupsaathi.entity.FollowUpStatus;
import com.followupsaathi.repository.EnquiryRepository;
import com.followupsaathi.repository.FollowUpRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregate report numbers for the reports screen. Every "month" boundary is
 * computed in the business's own time zone; the conversion rate divides by
 * created enquiries only and reports 0.0 for an empty period instead of NaN.
 */
@Service
public class ReportService {

    private final EnquiryRepository enquiryRepository;
    private final FollowUpRepository followUpRepository;
    private final BusinessService businessService;

    public ReportService(EnquiryRepository enquiryRepository,
            FollowUpRepository followUpRepository,
            BusinessService businessService) {
        this.enquiryRepository = enquiryRepository;
        this.followUpRepository = followUpRepository;
        this.businessService = businessService;
    }

    @Transactional(readOnly = true)
    public ReportDtos.View summary(Long userId, String rangeParam, String fromParam,
            String toParam) {
        var business = businessService.requireBusiness(userId);
        Long businessId = business.getId();
        ZoneId zone = business.zoneId();
        LocalDate today = LocalDate.now(zone);

        String range = rangeParam == null || rangeParam.isBlank()
                ? "THIS_MONTH" : rangeParam.trim().toUpperCase();
        LocalDate from;
        LocalDate to;
        switch (range) {
            case "THIS_WEEK" -> {
                from = today.minusDays(6);
                to = today;
            }
            case "LAST_30_DAYS" -> {
                from = today.minusDays(29);
                to = today;
            }
            case "THIS_MONTH" -> {
                from = today.withDayOfMonth(1);
                to = today;
            }
            default -> throw com.followupsaathi.exception.ApiException.badRequest(
                    "Unknown range: use THIS_MONTH, THIS_WEEK or LAST_30_DAYS");
        }

        if (fromParam != null && !fromParam.isBlank() && toParam != null
                && !toParam.isBlank()) {
            try {
                from = LocalDate.parse(fromParam.trim());
                to = LocalDate.parse(toParam.trim());
            } catch (java.time.format.DateTimeParseException ex) {
                throw com.followupsaathi.exception.ApiException.badRequest(
                        "from/to must be YYYY-MM-DD");
            }
            if (to.isBefore(from)) {
                throw com.followupsaathi.exception.ApiException.badRequest(
                        "to date cannot be before from date");
            }
        }

        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zone).toInstant();

        long created = enquiryRepository.countByBusinessIdAndCreatedAtBetween(
                businessId, fromInstant, toInstant);
        long convertedInWindow = enquiryRepository
                .countByBusinessIdAndArchivedFalseAndConvertedAtBetween(
                        businessId, fromInstant, toInstant);
        long followUpsCompleted = followUpRepository
                .countByEnquiryBusinessIdAndStatusAndCompletedAtBetween(
                        businessId, FollowUpStatus.COMPLETED, fromInstant, toInstant);
        long followUpsScheduled = followUpRepository
                .countByEnquiryBusinessIdAndCreatedAtBetween(
                        businessId, fromInstant, toInstant);

        long active = enquiryRepository.countByBusinessIdAndArchivedFalse(businessId);
        long archived = enquiryRepository.countByBusinessIdAndArchivedTrue(businessId);
        long pending = followUpRepository.countByEnquiryBusinessIdAndStatus(
                businessId, FollowUpStatus.PENDING);

        Double conversionRate = created == 0 ? 0.0
                : Math.round(convertedInWindow * 1000.0 / created) / 10.0;

        List<ReportDtos.StatusCount> statusCounts = new ArrayList<>();
        for (EnquiryStatus status : EnquiryStatus.values()) {
            statusCounts.add(new ReportDtos.StatusCount(status.name(),
                    enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                            businessId, status)));
        }

        return new ReportDtos.View(range, from.toString(), to.toString(),
                created, convertedInWindow, followUpsCompleted, followUpsScheduled,
                pending, active, archived, conversionRate, statusCounts);
    }

    /**
     * Same summary as CSV for the reports screen's export button. Values are
     * counts, dates and fixed labels only, and text cells are still
     * neutralized against spreadsheet formula injection.
     */
    @Transactional(readOnly = true)
    public String summaryCsv(Long userId, String rangeParam, String fromParam,
            String toParam) {
        ReportDtos.View v = summary(userId, rangeParam, fromParam, toParam);
        StringBuilder sb = new StringBuilder("metric,value\r\n");
        row(sb, "range", v.range());
        row(sb, "from_date", v.fromDate());
        row(sb, "to_date", v.toDate());
        row(sb, "enquiries_created", v.enquiriesCreated());
        row(sb, "enquiries_converted", v.enquiriesConverted());
        row(sb, "follow_ups_completed", v.followUpsCompleted());
        row(sb, "follow_ups_scheduled", v.followUpsScheduled());
        row(sb, "pending_follow_ups", v.pendingFollowUps());
        row(sb, "active_enquiries", v.activeEnquiries());
        row(sb, "archived_enquiries", v.archivedEnquiries());
        row(sb, "conversion_rate_percent", v.conversionRatePercent());
        for (ReportDtos.StatusCount sc : v.statusCounts()) {
            row(sb, "status_" + sc.status().toLowerCase(), sc.count());
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, String metric, Object value) {
        String cell = String.valueOf(value);
        if (cell.startsWith("=") || cell.startsWith("+") || cell.startsWith("-")
                || cell.startsWith("@")) {
            cell = "'" + cell;
        }
        sb.append(metric).append(',').append(cell).append("\r\n");
    }
}
