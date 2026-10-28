package com.followupsaathi.repository;

import com.followupsaathi.entity.MessageTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    List<MessageTemplate> findByBusinessIdOrderByIdAsc(Long businessId);

    Optional<MessageTemplate> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndTemplateNameIgnoreCase(Long businessId, String templateName);

    /** Removes every template of a business during account deletion. */
    void deleteByBusinessId(Long businessId);
}
