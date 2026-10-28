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
import java.time.Instant;

/**
 * A message the owner actually sent by opening WhatsApp/SMS themselves.
 * Recorded for history so the owner can see what was already communicated.
 */
@Entity
@Table(
    name = "message_logs",
    indexes = {
        @Index(name = "idx_message_logs_enquiry", columnList = "enquiry_id"),
        @Index(name = "idx_message_logs_sent_at", columnList = "sent_at")
    }
)
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    /** WHATSAPP or SMS — the app the owner opened. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageChannel channel;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public Long getId() {
        return id;
    }

    public Enquiry getEnquiry() {
        return enquiry;
    }

    public void setEnquiry(Enquiry enquiry) {
        this.enquiry = enquiry;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
