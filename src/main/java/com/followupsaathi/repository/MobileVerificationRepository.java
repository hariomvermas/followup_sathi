package com.followupsaathi.repository;

import com.followupsaathi.entity.MobileVerification;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileVerificationRepository extends JpaRepository<MobileVerification, Long> {

    Optional<MobileVerification> findFirstByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    /** A new code supersedes all older ones for the same number. */
    void deleteByPhoneNumber(String phoneNumber);

    void deleteByExpiresAtBefore(Instant cutoff);
}
