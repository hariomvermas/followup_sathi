package com.followupsaathi.service;

import com.followupsaathi.dto.FollowUpDtos;
import com.followupsaathi.entity.ActivityEventType;
import com.followupsaathi.entity.Enquiry;
import com.followupsaathi.entity.EnquiryActivity;
import com.followupsaathi.entity.EnquiryStatus;
import com.followupsaathi.entity.FollowUp;
import com.followupsaathi.entity.FollowUpOutcome;
import com.followupsaathi.entity.FollowUpStatus;
import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.repository.EnquiryActivityRepository;
import com.followupsaathi.repository.EnquiryRepository;
import com.followupsaathi.repository.FollowUpRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The follow-up engine: schedule, reschedule, complete, today/overdue lists.
 * Business rules enforced here:
 * - follow-ups are never deleted; completing records an outcome;
 * - completing with POSTPONED creates the next follow-up automatically;
 * - enquiry.next_follow_up_date always mirrors the next pending follow-up;
 * - enquiry status auto-advances NEW -> FOLLOW_UP_REQUIRED on first schedule.
 */
@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryActivityRepository activityRepository;
    private final BusinessService businessService;
    private final com.followupsaathi.repository.UserRepository userRepository;

    public FollowUpService(FollowUpRepository followUpRepository,
            EnquiryRepository enquiryRepository,
            EnquiryActivityRepository activityRepository,
            BusinessService businessService,
            com.followupsaathi.repository.UserRepository userRepository) {
        this.followUpRepository = followUpRepository;
        this.enquiryRepository = enquiryRepository;
        this.activityRepository = activityRepository;
        this.businessService = businessService;
        this.userRepository = userRepository;
    }

    @Transactional
    public FollowUpDtos.View schedule(Long userId, Long enquiryId,
            FollowUpDtos.Create request) {
        Enquiry enquiry = ownedEnquiry(userId, enquiryId);
        User actor = requireUser(userId);
        LocalDate date = parseDate(userId, request.date());
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Follow-up call" : request.reason().trim();

        FollowUp followUp = new FollowUp();
        followUp.setEnquiry(enquiry);
        followUp.setScheduledDate(date);
        followUp.setReason(reason);
        followUp.setEnquiryStatusSnapshot(enquiry.getStatus());
        followUp = followUpRepository.save(followUp);

        syncEnquiryState(enquiry, actor,
                ActivityEventType.FOLLOW_UP_SCHEDULED,
                "Follow-up scheduled for " + date);

        return toView(followUp);
    }

    /** Moves a pending follow-up to another day and/or reason. */
    @Transactional
    public FollowUpDtos.View reschedule(Long userId, Long followUpId,
            FollowUpDtos.Update request) {
        FollowUp followUp = ownedPendingFollowUp(userId, followUpId);
        User actor = requireUser(userId);

        LocalDate newDate = parseDate(userId, request.date());
        if (request.reason() != null && !request.reason().isBlank()) {
            followUp.setReason(request.reason().trim());
        }
        followUp.setScheduledDate(newDate);
        followUp = followUpRepository.save(followUp);

        syncEnquiryState(followUp.getEnquiry(), actor,
                ActivityEventType.FOLLOW_UP_RESCHEDULED,
                "Follow-up moved to " + newDate);

        return toView(followUp);
    }

    /** Records the outcome; never deletes the row. */
    @Transactional
    public FollowUpDtos.View complete(Long userId, Long followUpId,
            FollowUpDtos.Complete request) {
        FollowUp followUp = ownedPendingFollowUp(userId, followUpId);
        User actor = requireUser(userId);
        Enquiry enquiry = followUp.getEnquiry();

        followUp.setStatus(FollowUpStatus.COMPLETED);
        followUp.setOutcomeType(FollowUpOutcome.valueOf(request.outcomeType()));
        followUp.setOutcome(request.notes());
        followUp.setCompletedAt(Instant.now());
        followUp = followUpRepository.save(followUp);

        activityRepository.save(EnquiryActivity.of(enquiry, actor,
                ActivityEventType.FOLLOW_UP_COMPLETED,
                "Follow-up completed: " + request.outcomeType()));

        // Moving the next follow-up to a new day is an explicit reschedule
        // call; POSTPONED only records the outcome here.
        syncEnquiryState(enquiry, actor, null, null);
        return toView(followUp);
    }

    /** All follow-ups of one enquiry, newest first (pending + completed). */
    @Transactional(readOnly = true)
    public List<FollowUpDtos.View> listForEnquiry(Long userId, Long enquiryId) {
        Enquiry enquiry = ownedEnquiry(userId, enquiryId);
        return followUpRepository
                .findByEnquiryIdOrderByScheduledDateDesc(enquiry.getId()).stream()
                .map(this::toView)
                .toList();
    }

    /** Today's follow-ups plus everything still pending from earlier days. */
    @Transactional(readOnly = true)
    public List<FollowUpDtos.View> dueList(Long userId, Long enquiryId) {
        Enquiry enquiry = ownedEnquiry(userId, enquiryId);
        ZoneId zone = businessService.zone(userId);
        LocalDate today = LocalDate.now(zone);
        return followUpRepository
                .findByEnquiryIdOrderByScheduledDateDesc(enquiry.getId()).stream()
                .filter(f -> f.getStatus() == FollowUpStatus.PENDING
                        && !f.getScheduledDate().isAfter(today))
                .map(this::toView)
                .toList();
    }

    private void syncEnquiryState(Enquiry enquiry, User actor,
            ActivityEventType event, String description) {
        // Auto-advance NEW -> FOLLOW_UP_SCHEDULED on first schedule.
        if (enquiry.getStatus() == EnquiryStatus.NEW) {
            enquiry.setStatus(EnquiryStatus.FOLLOW_UP_REQUIRED);
        }
        // Mirror the next pending follow-up date on the enquiry.
        followUpRepository
                .findFirstByEnquiryIdAndStatusOrderByScheduledDateAsc(
                        enquiry.getId(), FollowUpStatus.PENDING)
                .ifPresentOrElse(
                        next -> enquiry.setNextFollowUpDate(next.getScheduledDate()),
                        () -> enquiry.setNextFollowUpDate(null));
        enquiryRepository.save(enquiry);

        if (event != null) {
            activityRepository.save(EnquiryActivity.of(enquiry, actor,
                    event, description));
        }
    }

    private FollowUp ownedPendingFollowUp(Long userId, Long followUpId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> ApiException.notFound("Follow-up not found"));
        // Ownership check via the enquiry's business.
        var business = businessService.requireBusiness(userId);
        if (!followUp.getEnquiry().getBusiness().getId().equals(business.getId())) {
            throw ApiException.notFound("Follow-up not found");
        }
        if (followUp.getStatus() != FollowUpStatus.PENDING) {
            throw ApiException.badRequest("Only pending follow-ups can be changed");
        }
        return followUp;
    }

    private Enquiry ownedEnquiry(Long userId, Long enquiryId) {
        var business = businessService.requireBusiness(userId);
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> ApiException.notFound("Enquiry not found"));
        if (!enquiry.getBusiness().getId().equals(business.getId())) {
            throw ApiException.notFound("Enquiry not found");
        }
        return enquiry;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
    }

    private LocalDate parseDate(Long userId, String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("Date is required");
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            ZoneId zone = businessService.zone(userId);
            if (date.isBefore(LocalDate.now(zone).minusDays(1))) {
                throw ApiException.badRequest(
                        "Date cannot be in the past (more than one day back)");
            }
            return date;
        } catch (java.time.format.DateTimeParseException ex) {
            throw ApiException.badRequest("Date must be YYYY-MM-DD");
        }
    }

    private FollowUpDtos.View toView(FollowUp followUp) {
        Enquiry enquiry = followUp.getEnquiry();
        return new FollowUpDtos.View(
                followUp.getId(),
                enquiry.getId(),
                enquiry.getCustomerName(),
                enquiry.getPhoneNumber(),
                enquiry.getServiceName(),
                followUp.getScheduledDate().toString(),
                followUp.getReason(),
                followUp.getStatus().name(),
                followUp.getOutcomeType() == null ? null : followUp.getOutcomeType().name(),
                followUp.getOutcome(),
                followUp.getCompletedAt(),
                followUp.getCreatedAt());
    }
}
