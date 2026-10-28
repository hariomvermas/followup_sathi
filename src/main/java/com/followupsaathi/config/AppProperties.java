package com.followupsaathi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed application properties. Everything is optional with safe defaults so
 * the app runs out of the box in development.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Session session = new Session();
    private final Verification verification = new Verification();

    public Session getSession() {
        return session;
    }

    public Verification getVerification() {
        return verification;
    }

    public static class Session {
        /** Cookie marked Secure only when the app runs behind HTTPS. */
        private boolean cookieSecure = false;

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }
    }

    public static class Verification {
        /** How long a mobile verification code stays valid. */
        private int codeValidMinutes = 10;

        /** Minimum delay between two codes for the same number. */
        private int resendCooldownSeconds = 60;

        public int getCodeValidMinutes() {
            return codeValidMinutes;
        }

        public void setCodeValidMinutes(int codeValidMinutes) {
            this.codeValidMinutes = codeValidMinutes;
        }

        public int getResendCooldownSeconds() {
            return resendCooldownSeconds;
        }

        public void setResendCooldownSeconds(int resendCooldownSeconds) {
            this.resendCooldownSeconds = resendCooldownSeconds;
        }
    }
}
