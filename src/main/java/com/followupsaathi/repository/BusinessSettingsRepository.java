package com.followupsaathi.repository;

import com.followupsaathi.entity.BusinessSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {

    Optional<BusinessSettings> findByBusinessId(Long businessId);

    @Modifying
    @Query("delete from BusinessSettings s where s.business.id = :businessId")
    void deleteByBusinessId(@Param("businessId") Long businessId);
}
