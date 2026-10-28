package com.followupsaathi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A pending one-time code for verifying the mobile number entered during
 * registration. v1 sends the code over WhatsApp or SMS by opening the
 * messaging app with a prefilled message; the code is shown in the UI so the
 * owner can read it out or test quickly. Expired/used codes are deleted.
 */
@Entity
@Table(
    name = "mobile_verifications",
    indexes = {
        @Index(name = "idx_mobile_verifications_phone", columnList = "phone_number"),
        @Index(name = "idx_mobile_verifications_expires_at", columnList = "expires_at")
    }
)
public class MobileVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalized 10-digit Indian mobile number, digits only. */
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** Six-digit numeric code, stored as string to preserve leading zeros. */
    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
