package com.followupsaathi.config;

/** Fixed product constants. Version-one scope is deliberately small. */
public final class AppConstants {

    private AppConstants() {
    }

    /** Default business time zone when none is configured. */
    public static final String DEFAULT_TIME_ZONE = "Asia/Kolkata";

    /** Default reminder slot when none is configured. */
    public static final java.time.LocalTime DEFAULT_REMINDER_TIME =
            java.time.LocalTime.of(10, 0);

    /** Default working days (Monday-Saturday, ISO day numbers). */
    public static final String DEFAULT_WORKING_DAYS = "1,2,3,4,5,6";

    /** Preferred language used when onboarding does not specify one. */
    public static final String DEFAULT_LANGUAGE = "en";

    /** Page sizes are bounded to keep list responses small. */
    public static final int MAX_PAGE_SIZE = 50;
}
