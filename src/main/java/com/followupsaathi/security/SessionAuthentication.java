package com.followupsaathi.security;

import com.followupsaathi.entity.UserSession;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication produced by SessionAuthenticationFilter for a valid session
 * cookie. Carries the session row so request handling can refresh activity
 * timestamps without another query.
 */
public class SessionAuthentication extends AbstractAuthenticationToken {

    private final AppUserDetails principal;
    private final UserSession session;

    public SessionAuthentication(AppUserDetails principal, UserSession session) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.session = session;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // the credential (cookie token) is never re-exposed
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public AppUserDetails getAppUserDetails() {
        return principal;
    }

    public UserSession getSession() {
        return session;
    }
}
