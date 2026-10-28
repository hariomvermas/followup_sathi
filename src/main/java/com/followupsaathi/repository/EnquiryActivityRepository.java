package com.followupsaathi.repository;

import com.followupsaathi.entity.EnquiryActivity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryActivityRepository extends JpaRepository<EnquiryActivity, Long> {

    Page<EnquiryActivity> findByEnquiryIdOrderByCreatedAtDesc(Long enquiryId, Pageable pageable);

    /** Small timelines are fetched whole, newest first. */
    List<EnquiryActivity> findTop50ByEnquiryIdOrderByCreatedAtDesc(Long enquiryId);
}
