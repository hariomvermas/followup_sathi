package com.followupsaathi.entity;

/**
 * Where an enquiry came from. Stored as a VARCHAR so new sources can be added
 * without a schema migration.
 */
public enum EnquirySource {
    PHONE_CALL,
    WALK_IN,
    REFERRAL,
    WEBSITE,
    MESSAGING_APP,
    OTHER
}
