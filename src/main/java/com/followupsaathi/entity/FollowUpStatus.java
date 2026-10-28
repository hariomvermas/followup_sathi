package com.followupsaathi.entity;

/**
 * Lifecycle of a follow-up. COMPLETED follow-ups keep their outcome and stay
 * in history; they are never deleted.
 */
public enum FollowUpStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
