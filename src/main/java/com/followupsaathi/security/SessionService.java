package com.followupsaathi.security;

import com.followupsaathi.entity.User;
import com.followupsaathi.entity.UserSession;
import com.followupsaathi.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns server-side sessions. The browser holds only an opaque random token in
 * an HttpOnly cookie; the database stores the SHA-256 hash of that token, so
 * a database leak does not immediately yield usable session cookies.
 * Sessions survive application restarts by design.
 */
@Service
public class SessionService {

    public static final String SESSION_COOKIE = "FUSAHI_SESSION";
    private static final Duration SESSION_TTL = Duration.ofDays(30);
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(5);

    private final UserSessionRepository sessionRepository;
    private final SecureRandom random = new SecureRandom();
    private final boolean cookieSecure;

    public SessionService(UserSessionRepository sessionRepository,
            @Value("${app.session.cookie-secure:false}") boolean cookieSecure) {
        this.sessionRepository = sessionRepository;
        this.cookieSecure = cookieSecure;
    }

    /** Creates a session row and sends the session cookie. */
    @Transactional
    public void create(User user, HttpServletResponse response) {
        String token = newToken();
        Instant now = Instant.now();

        UserSession session = new UserSession();
        session.setSessionId(hashToken(token));
        session.setUser(user);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(SESSION_TTL));
        session.setLastSeenAt(now);
        sessionRepository.save(session);

        // Opportunistic cleanup keeps the table small without a scheduler.
        sessionRepository.deleteByExpiresAtBefore(now.minus(Duration.ofDays(1)));

        writeCookie(response, token, SESSION_TTL);
    }

    /** Deletes the session row and expires the cookie. */
    @Transactional
    public void delete(HttpServletRequest request, HttpServletResponse response) {
        String token = readCookieToken(request);
        if (token != null) {
            sessionRepository.findById(hashToken(token)).ifPresent(sessionRepository::delete);
        }
        writeCookie(response, "", Duration.ZERO);
    }

    /** Hashes the raw cookie token for storage/lookup. */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /** Reads the raw session token from the request, or null. */
    public static String readCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .secure(cookieSecure)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
