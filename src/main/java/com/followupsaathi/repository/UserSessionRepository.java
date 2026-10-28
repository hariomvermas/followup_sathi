package com.followupsaathi.repository;

import com.followupsaathi.entity.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    /**
     * Loads an unexpired session together with its user (join fetch, because
     * the association is lazy and the auth filter runs outside a transaction).
     */
    @Query("""
            select s from UserSession s
            join fetch s.user
            where s.sessionId = :sessionId and s.expiresAt > :now
            """)
    Optional<UserSession> findActiveWithUser(@Param("sessionId") String sessionId,
            @Param("now") Instant now);

    /** Purge job: removes sessions that expired before the cutoff. */
    void deleteByExpiresAtBefore(Instant cutoff);

    void deleteByUserId(Long userId);
}
