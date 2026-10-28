package com.followupsaathi.repository;

import com.followupsaathi.entity.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Filtering (status, search text, archived flag, due date) is composed
 * dynamically with Specifications, so every combination stays null-safe.
 */
public interface EnquiryRepository
        extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {

    long countByBusinessIdAndArchivedFalse(Long businessId);

    long countByBusinessIdAndStatus(Long businessId, com.followupsaathi.entity.EnquiryStatus status);

    long countByBusinessIdAndArchivedFalseAndStatus(
            Long businessId, com.followupsaathi.entity.EnquiryStatus status);

    long countByBusinessIdAndArchivedTrue(Long businessId);

    /** Enquiries created inside a reporting window (any archived state). */
    long countByBusinessIdAndCreatedAtBetween(
            Long businessId, java.time.Instant from, java.time.Instant to);

    /** Enquiries created inside a window and still not archived. */
    long countByBusinessIdAndArchivedFalseAndCreatedAtBetween(
            Long businessId, java.time.Instant from, java.time.Instant to);

    /** Conversions whose converted_at falls inside a window. */
    long countByBusinessIdAndArchivedFalseAndConvertedAtBetween(
            Long businessId, java.time.Instant from, java.time.Instant to);

    /** Removes every enquiry of a business during account deletion. */
    void deleteByBusinessId(Long businessId);
}
