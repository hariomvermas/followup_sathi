package com.followupsaathi.security;

import tools.jackson.databind.ObjectMapper;
import com.followupsaathi.exception.ErrorResponse;
import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Security wiring: stateless-in-spirit cookie sessions backed by the
 * user_sessions table, SPA-friendly CSRF, and JSON error bodies instead of
 * redirects for the frontend to handle.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final SessionLogoutHandler sessionLogoutHandler;
    private final ObjectMapper objectMapper;

    public SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter,
            SessionLogoutHandler sessionLogoutHandler,
            ObjectMapper objectMapper) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.sessionLogoutHandler = sessionLogoutHandler;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Exposed as a bean so /api/auth/csrf can hand out the raw cookie value. */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        CsrfTokenRequestAttributeHandler rawHandler = new CsrfTokenRequestAttributeHandler();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/api/auth/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/css/**",
                                "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login",
                                "/api/auth/csrf").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterAfter(sessionAuthenticationFilter,
                        SecurityContextHolderFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(this::writeJsonError)
                        .accessDeniedHandler(this::writeJsonError))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(sessionLogoutHandler)
                        .logoutSuccessHandler(this::writeLogoutSuccess));

        return http.build();
    }

    private void writeJsonError(HttpServletRequest request,
            HttpServletResponse response, Exception exception) throws IOException {
        int status = exception instanceof org.springframework.security.access.AccessDeniedException
                ? HttpStatus.FORBIDDEN.value()
                : HttpStatus.UNAUTHORIZED.value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("status", status, "error",
                        status == 403 ? "Forbidden" : "Unauthorized",
                        "message", "Authentication required",
                        "path", request.getRequestURI()));
    }

    private void writeLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", "Logged out"));
    }
}
