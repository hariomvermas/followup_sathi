package com.followupsaathi.repository;

import com.followupsaathi.entity.Business;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}
