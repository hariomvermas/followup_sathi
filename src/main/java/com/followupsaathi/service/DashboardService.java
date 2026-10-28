package com.followupsaathi.service;

import com.followupsaathi.dto.DashboardDtos;
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
 * Dashboard counters. Every "today" is computed in the business's own time
 * zone (default IST), never the server's, and counts are scoped to one
 * business so numbers are never shared across accounts.
 */
@Service
public class DashboardService {

    private final EnquiryRepository enquiryRepository;
    private final FollowUpRepository followUpRepository;
    private final BusinessService businessService;
    private final com.followupsaathi.mapper.EnquiryMapper mapper;

    public DashboardService(EnquiryRepository enquiryRepository,
            FollowUpRepository followUpRepository,
            BusinessService businessService,
            com.followupsaathi.mapper.EnquiryMapper mapper) {
        this.enquiryRepository = enquiryRepository;
        this.followUpRepository = followUpRepository;
        this.businessService = businessService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.View view(Long userId) {
        Long businessId = businessService.requireBusiness(userId).getId();
        ZoneId zone = businessService.zone(userId);
        LocalDate today = LocalDate.now(zone);

        long total = enquiryRepository.countByBusinessIdAndArchivedFalse(businessId);
        long newCount = enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                businessId, EnquiryStatus.NEW);
        long contacted = enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                businessId, EnquiryStatus.CONTACTED);
        long interested = enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                businessId, EnquiryStatus.INTERESTED);
        long converted = enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                businessId, EnquiryStatus.CONVERTED);
        long followUpRequired = enquiryRepository.countByBusinessIdAndArchivedFalseAndStatus(
                businessId, EnquiryStatus.FOLLOW_UP_REQUIRED);

        long dueToday = followUpRepository.countByEnquiryBusinessIdAndStatusAndScheduledDate(
                businessId, FollowUpStatus.PENDING, today);
        long overdue = followUpRepository.countByEnquiryBusinessIdAndStatusAndScheduledDateLessThanEqual(
                businessId, FollowUpStatus.PENDING, today.minusDays(1));

        Instant weekStart = today.minusDays(6).atStartOfDay(zone).toInstant();
        Instant weekEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        long completedThisWeek = followUpRepository
                .countByEnquiryBusinessIdAndStatusAndCompletedAtBetween(
                        businessId, FollowUpStatus.COMPLETED, weekStart, weekEnd);

        List<DashboardDtos.FollowUpItem> todayItems = new ArrayList<>();
        followUpRepository
                .findByEnquiryBusinessIdAndStatusAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
                        businessId, FollowUpStatus.PENDING, today)
                .forEach(f -> {
                    var enquiry = f.getEnquiry();
                    todayItems.add(new DashboardDtos.FollowUpItem(
                            f.getId(),
                            enquiry.getId(),
                            enquiry.getCustomerName(),
                            enquiry.getPhoneNumber(),
                            enquiry.getServiceName(),
                            f.getScheduledDate().toString(),
                            f.getReason(),
                            f.isOverdue(today)));
                });

        return new DashboardDtos.View(total, newCount, contacted, interested,
                converted, followUpRequired, dueToday, overdue, completedThisWeek, todayItems);
    }
}
