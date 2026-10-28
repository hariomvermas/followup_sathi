package com.followupsaathi.repository;

import com.followupsaathi.entity.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    Page<MessageLog> findByEnquiryIdOrderBySentAtDesc(Long enquiryId, Pageable pageable);
}
