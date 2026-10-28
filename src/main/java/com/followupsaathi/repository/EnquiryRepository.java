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
}
