package com.followupsaathi.security;

import com.followupsaathi.entity.UserSession;
import com.followupsaathi.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests by looking up the session cookie's hash in the
 * user_sessions table. Requests without a valid session simply stay
 * unauthenticated and are handled by the security filter chain.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final UserSessionRepository sessionRepository;

    public SessionAuthenticationFilter(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = SessionService.readCookieToken(request);
        if (token != null && SecurityContextHolderAccess.getAuth() == null) {
            sessionRepository.findActiveWithUser(SessionService.hashToken(token), Instant.now())
                    .ifPresent(session -> authenticate(session, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(UserSession session, HttpServletRequest request) {
        if (!session.getUser().isEnabled()) {
            return;
        }
        AppUserDetails principal = new AppUserDetails(session.getUser());
        SessionAuthentication authentication = new SessionAuthentication(
                principal, session);
        SecurityContextHolderAccess.setAuth(authentication);
    }

    /** Tiny holder so the filter stays a plain component. */
    static final class SecurityContextHolderAccess {
        static org.springframework.security.core.Authentication getAuth() {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
        }

        static void setAuth(org.springframework.security.core.Authentication authentication) {
            org.springframework.security.core.context.SecurityContextHolder
                    .getContext().setAuthentication(authentication);
        }
    }
}
