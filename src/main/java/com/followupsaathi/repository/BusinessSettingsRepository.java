package com.followupsaathi.repository;

import com.followupsaathi.entity.BusinessSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {

    Optional<BusinessSettings> findByBusinessId(Long businessId);
}
