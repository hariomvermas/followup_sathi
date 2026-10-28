package com.followupsaathi.entity;

/**
 * Kinds of events recorded in an enquiry's history. One row is inserted per
 * real user action — nothing is ever synthesized.
 */
public enum ActivityEventType {
    ENQUIRY_CREATED,
    ENQUIRY_UPDATED,
    STATUS_CHANGED,
    ENQUIRY_CONVERTED,
    ENQUIRY_ARCHIVED,
    ENQUIRY_REOPENED,
    FOLLOW_UP_SCHEDULED,
    FOLLOW_UP_RESCHEDULED,
    FOLLOW_UP_COMPLETED,
    NOTE_ADDED
}
