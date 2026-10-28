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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Per-business preferences captured during onboarding and editable in
 * Settings. Kept in a separate table so Business stays a pure profile record.
 * The reminder time is the fixed wall-clock slot when follow-ups are meant to
 * be done each day.
 */
@Entity
@Table(name = "business_settings")
public class BusinessSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true)
    private Business business;

    /**
     * Whether the dashboard should surface due/overdue follow-ups as a
     * prominent "Today" list. In-app only in version one.
     */
    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    /** Wall-clock time of the daily follow-up slot, default 10:00. */
    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(10, 0);

    /**
     * Preferred hour for message-appointment suggestions; informational in
     * version one.
     */
    @Column(name = "working_hours_start", nullable = false)
    private LocalTime workingHoursStart = LocalTime.of(9, 0);

    @Column(name = "working_hours_end", nullable = false)
    private LocalTime workingHoursEnd = LocalTime.of(19, 0);

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

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public LocalTime getWorkingHoursStart() {
        return workingHoursStart;
    }

    public void setWorkingHoursStart(LocalTime workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }

    public LocalTime getWorkingHoursEnd() {
        return workingHoursEnd;
    }

    public void setWorkingHoursEnd(LocalTime workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
