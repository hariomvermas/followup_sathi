package com.followupsaathi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One scheduled follow-up call for an enquiry. Follow-ups are never deleted;
 * completing one records the outcome and keeps it in history. "Overdue" is
 * always derived (scheduled date before today, still PENDING) and never a
 * stored flag, so it can never drift out of sync with reality.
 */
@Entity
@Table(
    name = "follow_ups",
    indexes = {
        @Index(name = "idx_follow_ups_enquiry", columnList = "enquiry_id"),
        @Index(name = "idx_follow_ups_enquiry_status", columnList = "enquiry_id, status"),
        @Index(name = "idx_follow_ups_scheduled", columnList = "scheduled_date")
    }
)
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    /** Denormalized status for fast dashboard queries; mirrors enquiry status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "enquiry_status_snapshot", nullable = false, length = 20)
    private EnquiryStatus enquiryStatusSnapshot;

    /** Why this follow-up exists, e.g. "Ask about admission decision". */
    @Column(nullable = false, length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status = FollowUpStatus.PENDING;

    /** Free-text outcome recorded when the follow-up is completed. */
    @Column(columnDefinition = "TEXT")
    private String outcome;

    /** Structured outcome picked from a fixed list, for counting later. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_type", length = 20)
    private FollowUpOutcome outcomeType;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Enquiry getEnquiry() {
        return enquiry;
    }

    public void setEnquiry(Enquiry enquiry) {
        this.enquiry = enquiry;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public EnquiryStatus getEnquiryStatusSnapshot() {
        return enquiryStatusSnapshot;
    }

    public void setEnquiryStatusSnapshot(EnquiryStatus enquiryStatusSnapshot) {
        this.enquiryStatusSnapshot = enquiryStatusSnapshot;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FollowUpStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpStatus status) {
        this.status = status;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public FollowUpOutcome getOutcomeType() {
        return outcomeType;
    }

    public void setOutcomeType(FollowUpOutcome outcomeType) {
        this.outcomeType = outcomeType;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** True when the scheduled day has passed and the follow-up is still open. */
    public boolean isOverdue(LocalDate today) {
        return status == FollowUpStatus.PENDING && scheduledDate.isBefore(today);
    }

    /** True when the follow-up falls on the given day. */
    public boolean isDueOn(LocalDate day) {
        return status == FollowUpStatus.PENDING && scheduledDate.isEqual(day);
    }
}
