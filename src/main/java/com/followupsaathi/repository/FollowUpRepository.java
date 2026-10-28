package com.followupsaathi.repository;

import com.followupsaathi.entity.FollowUp;
import com.followupsaathi.entity.FollowUpStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    /** Due or overdue: scheduled on or before the given day, still pending. */
    List<FollowUp> findByEnquiryBusinessIdAndStatusAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
            Long businessId, FollowUpStatus status, LocalDate date);

    /** Follow-ups scheduled on exactly one day. */
    List<FollowUp> findByEnquiryBusinessIdAndStatusAndScheduledDateOrderByScheduledDateAsc(
            Long businessId, FollowUpStatus status, LocalDate date);

    /** History for one enquiry, newest first. */
    Page<FollowUp> findByEnquiryIdOrderByScheduledDateDesc(Long enquiryId, Pageable pageable);

    /** All follow-ups of an enquiry, newest first (small lists). */
    List<FollowUp> findByEnquiryIdOrderByScheduledDateDesc(Long enquiryId);

    /** The next pending follow-up of an enquiry, if any. */
    Optional<FollowUp> findFirstByEnquiryIdAndStatusOrderByScheduledDateAsc(
            Long enquiryId, FollowUpStatus status);

    long countByEnquiryBusinessIdAndStatusAndScheduledDateLessThanEqual(
            Long businessId, FollowUpStatus status, LocalDate date);

    long countByEnquiryBusinessIdAndStatusAndScheduledDate(
            Long businessId, FollowUpStatus status, LocalDate date);

    long countByEnquiryBusinessIdAndStatusAndCompletedAtBetween(
            Long businessId, FollowUpStatus status, Instant from, Instant to);

    /** Removes an enquiry's follow-ups when the enquiry is deleted. */
    void deleteByEnquiryId(Long enquiryId);

    /** Follow-ups created inside a reporting window. */
    long countByEnquiryBusinessIdAndCreatedAtBetween(Long businessId, Instant from, Instant to);

    /** Current pending follow-ups of a business (any date). */
    long countByEnquiryBusinessIdAndStatus(Long businessId, FollowUpStatus status);

    /** All pending follow-ups of a business, soonest first. */
    List<FollowUp> findByEnquiryBusinessIdAndStatusOrderByScheduledDateAsc(
            Long businessId, FollowUpStatus status);

    /** Removes every follow-up of a business during account deletion. */
    void deleteByEnquiryBusinessId(Long businessId);
}
