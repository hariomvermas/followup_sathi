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
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A customer enquiry: a person who showed interest in the business.
 * Every enquiry belongs to exactly one business; the API never accepts a
 * business id from the client, it is always taken from the authenticated user.
 */
@Entity
@Table(
    name = "enquiries",
    indexes = {
        @Index(name = "idx_enquiries_business", columnList = "business_id"),
        @Index(name = "idx_enquiries_business_status", columnList = "business_id, status"),
        @Index(name = "idx_enquiries_next_follow_up", columnList = "business_id, next_follow_up_date"),
        @Index(name = "idx_enquiries_phone", columnList = "phone_number"),
        @Index(name = "idx_enquiries_customer_name", columnList = "customer_name")
    }
)
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @NotBlank
    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 180)
    private String email;

    /** Service of interest, e.g. "Class 10 Maths batch". */
    @Column(name = "service_name", length = 140)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnquirySource source = EnquirySource.PHONE_CALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EnquiryPriority priority = EnquiryPriority.MEDIUM;

    /**
     * Business-local calendar date of the next scheduled follow-up. Kept as a
     * date because owners think in days; the reminder slot time comes from
     * BusinessSettings. Kept in sync when follow-ups are scheduled.
     */
    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Archival is a soft delete: rows stay queryable in history. */
    @Column(nullable = false)
    private boolean archived = false;

    /** Set when the enquiry first reaches CONVERTED, for reporting. */
    @Column(name = "converted_at")
    private Instant convertedAt;

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public EnquirySource getSource() {
        return source;
    }

    public void setSource(EnquirySource source) {
        this.source = source;
    }

    public EnquiryStatus getStatus() {
        return status;
    }

    public void setStatus(EnquiryStatus status) {
        this.status = status;
    }

    public EnquiryPriority getPriority() {
        return priority;
    }

    public void setPriority(EnquiryPriority priority) {
        this.priority = priority;
    }

    public LocalDate getNextFollowUpDate() {
        return nextFollowUpDate;
    }

    public void setNextFollowUpDate(LocalDate nextFollowUpDate) {
        this.nextFollowUpDate = nextFollowUpDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Marks the enquiry converted, stamping the conversion time once. */
    public void markConverted(Instant when) {
        this.status = EnquiryStatus.CONVERTED;
        if (this.convertedAt == null) {
            this.convertedAt = when;
        }
    }
}
