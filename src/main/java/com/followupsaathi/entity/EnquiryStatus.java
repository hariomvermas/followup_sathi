package com.followupsaathi.entity;

/**
 * Lifecycle of an enquiry. The order of the constants matters: CONVERSION is
 * defined as reaching the CONVERTED state, and status transitions are validated
 * in the service layer.
 */
public enum EnquiryStatus {
    NEW,
    CONTACTED,
    INTERESTED,
    FOLLOW_UP_REQUIRED,
    CONVERTED,
    NOT_INTERESTED
}
