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
import java.time.Instant;

/**
 * One event in an enquiry's history. Rows are only written by real user
 * actions; nothing is synthesized. Each row stores which user performed the
 * action, as required by the product spec.
 */
@Entity
@Table(
    name = "enquiry_activities",
    indexes = {
        @Index(name = "idx_activities_enquiry", columnList = "enquiry_id"),
        @Index(name = "idx_activities_enquiry_time", columnList = "enquiry_id, created_at")
    }
)
public class EnquiryActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    /** The authenticated user who performed the action. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ActivityEventType eventType;

    /** Human-readable description shown in the history timeline. */
    @Column(nullable = false, length = 400)
    private String description;

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

    public Enquiry getEnquiry() {
        return enquiry;
    }

    public void setEnquiry(Enquiry enquiry) {
        this.enquiry = enquiry;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public void setEventType(ActivityEventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Convenience factory used by the service layer. */
    public static EnquiryActivity of(Enquiry enquiry, User actor,
            ActivityEventType type, String description) {
        EnquiryActivity activity = new EnquiryActivity();
        activity.enquiry = enquiry;
        activity.actor = actor;
        activity.eventType = type;
        activity.description = description;
        return activity;
    }
}
