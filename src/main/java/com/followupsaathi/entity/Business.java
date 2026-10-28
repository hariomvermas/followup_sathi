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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The business account behind a user. Created during onboarding; owns every
 * enquiry. All data isolation in the API is derived from this relationship.
 */
@Entity
@Table(
    name = "businesses",
    indexes = {
        @Index(name = "idx_businesses_owner", columnList = "owner_id"),
        @Index(name = "idx_businesses_name", columnList = "business_name")
    }
)
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "business_name", nullable = false, length = 140)
    private String businessName;

    /** Free category label, e.g. Coaching centre, Salon, Service provider. */
    @Column(name = "business_category", length = 80)
    private String businessCategory;

    /** Two-letter language code for template suggestions, default "en". */
    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage = "en";

    /** IANA zone used for every "today"/overdue computation. Default IST. */
    @Column(nullable = false, length = 40)
    private String timeZone = "Asia/Kolkata";

    /** Monday-first day list, default Mon..Sat: 1,2,3,4,5,6 */
    @Column(name = "working_days", nullable = false, length = 13)
    private String workingDays = "1,2,3,4,5,6";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "business", fetch = FetchType.LAZY)
    private List<Enquiry> enquiries = new ArrayList<>();

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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(String workingDays) {
        this.workingDays = workingDays;
    }

    /** Safe accessor: returns the parsed zone, falling back to IST if invalid. */
    public ZoneId zoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (RuntimeException ex) {
            return ZoneId.of("Asia/Kolkata");
        }
    }
}
