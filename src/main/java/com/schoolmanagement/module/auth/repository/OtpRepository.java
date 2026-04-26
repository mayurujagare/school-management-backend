package com.schoolmanagement.module.auth.repository;

import com.schoolmanagement.module.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, UUID> {

    // Latest unused, unexpired OTP for this identifier
    @Query("""
        SELECT o FROM OtpVerification o
        WHERE o.identifier = :identifier
        AND o.isUsed = false
        AND o.expiresAt > :now
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestValid(String identifier, LocalDateTime now);

    // Count OTP requests in last 15 minutes (rate limiting)
    @Query("""
        SELECT COUNT(o) FROM OtpVerification o
        WHERE o.identifier = :identifier
        AND o.createdAt > :since
    """)
    long countRecentRequests(String identifier, LocalDateTime since);
}