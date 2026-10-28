package com.followupsaathi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Deletes the server-side session row and expires the cookie on logout. */
@Component
public class SessionLogoutHandler implements LogoutHandler {

    private final SessionService sessionService;

    public SessionLogoutHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        sessionService.delete(request, response);
    }
}
