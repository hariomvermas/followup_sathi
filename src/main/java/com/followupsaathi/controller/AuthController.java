package com.followupsaathi.controller;

import com.followupsaathi.dto.AuthDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration, login, logout and session lookup. The CSRF endpoint lets the
 * SPA obtain a token before the first login: the cookie is set automatically,
 * the JSON body mirrors it so the frontend can also read it directly.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthService authService, CurrentUser currentUser,
            CsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.currentUser = currentUser;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletResponse response) {
        return authService.register(request, response);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        // Handled by Spring Security's logout filter; this mapping exists so
        // the endpoint also responds JSON when invoked without a session.
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public AuthDtos.AuthResponse me() {
        return authService.me(currentUser.requireUserId());
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token, HttpServletRequest request,
            HttpServletResponse response) {
        // Touching the token materializes the deferred XSRF-TOKEN cookie.
        // Spring Security defers token generation, so load/generate it from
        // the repository directly: the raw value here is exactly what the
        // SPA must send back in the X-XSRF-TOKEN header.
        CsrfToken loaded = csrfTokenRepository.loadToken(request);
        if (loaded == null) {
            loaded = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(loaded, request, response);
        }
        return Map.of(
                "headerName", loaded.getHeaderName(),
                "token", loaded.getToken());
    }
}
