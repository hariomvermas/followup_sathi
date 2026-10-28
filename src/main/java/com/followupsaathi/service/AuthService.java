package com.followupsaathi.service;

import com.followupsaathi.config.TemplateDefaults;
import com.followupsaathi.dto.AuthDtos;
import com.followupsaathi.entity.Business;
import com.followupsaathi.entity.MessageTemplate;
import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.exception.ValidationException;
import com.followupsaathi.mapper.BusinessMapper;
import com.followupsaathi.repository.BusinessRepository;
import com.followupsaathi.repository.MessageTemplateRepository;
import com.followupsaathi.repository.UserRepository;
import com.followupsaathi.security.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and session lookup. On successful registration the
 * account, business profile and default message templates are created in one
 * transaction, so onboarding never leaves half-initialized data behind.
 */
@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9+\\-\\s]{0,20}$");

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final MessageTemplateRepository templateRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.followupsaathi.security.LoginRateLimiter rateLimiter;
    private final com.followupsaathi.security.SessionService sessionService;

    public AuthService(UserRepository userRepository,
            BusinessRepository businessRepository,
            MessageTemplateRepository templateRepository,
            PasswordEncoder passwordEncoder,
            com.followupsaathi.security.LoginRateLimiter rateLimiter,
            com.followupsaathi.security.SessionService sessionService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.templateRepository = templateRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.sessionService = sessionService;
    }

    /** Creates the account, business profile and seed templates. */
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request,
            HttpServletResponse response) {
        ValidationException problems = new ValidationException(
                "Please correct the highlighted fields");

        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String mobile = normalizeMobile(request.mobile());
        String password = request.password() == null ? "" : request.password();

        if (name.isEmpty()) {
            problems.add("name", "Name is required");
        } else if (name.length() > 120) {
            problems.add("name", "Name must be at most 120 characters");
        }
        if (email.isEmpty()) {
            problems.add("email", "Email is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            problems.add("email", "Enter a valid email address");
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            problems.add("email", "An account with this email already exists");
        }
        if (mobile != null && !MOBILE_PATTERN.matcher(mobile).matches()) {
            problems.add("mobile", "Mobile can contain only digits, spaces, + and -");
        }
        if (password.length() < 8) {
            problems.add("password", "Password must be at least 8 characters");
        }
        if (!password.equals(request.confirmPassword())) {
            problems.add("confirmPassword", "Passwords do not match");
        }
        if (problems.hasErrors()) {
            throw problems;
        }

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);

        // A business shell is created immediately; the owner completes it in
        // the setup wizard, so the wizard can prefill nothing yet still save.
        Business business = new Business();
        business.setOwner(user);
        business.setBusinessName(name + "'s Business");
        business.setBusinessCategory("");
        business.setTimeZone(com.followupsaathi.config.AppConstants.DEFAULT_TIME_ZONE);
        business.setWorkingDays(com.followupsaathi.config.AppConstants.DEFAULT_WORKING_DAYS);
        businessRepository.save(business);

        List<MessageTemplate> templates = TemplateDefaults.forBusiness(business);
        templateRepository.saveAll(templates);

        // No session is created here: the owner logs in explicitly next.
        return new AuthDtos.AuthResponse(user.getId(), user.getFullName(),
                user.getEmail(), true, false);
    }

    /** Verifies credentials and starts a server-side session. */
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String key = rateLimitKey(request.email(), httpRequest);
        if (!rateLimiter.tryAcquire(key)) {
            throw ApiException.tooManyRequests(
                    "Too many failed attempts. Please wait a few minutes and try again.");
        }

        User user = userRepository.findByEmailIgnoreCase(
                request.email() == null ? "" : request.email().trim())
                .orElse(null);

        // Same error for unknown email and wrong password, per spec.
        if (user == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(key);
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("This account has been disabled");
        }

        rateLimiter.reset(key);
        sessionService.create(user, httpResponse);
        return toAuthResponse(user);
    }

    /** Returns the authenticated user's session info. */
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        return toAuthResponse(user);
    }

    private AuthDtos.AuthResponse toAuthResponse(User user) {
        boolean hasBusiness = businessRepository.existsByOwnerId(user.getId());
        boolean complete = false;
        if (hasBusiness) {
            Business business = businessRepository.findByOwnerId(user.getId()).orElse(null);
            complete = business != null
                    && business.getBusinessName() != null
                    && !business.getBusinessName().isBlank()
                    && business.getBusinessCategory() != null
                    && !business.getBusinessCategory().isBlank();
        }
        return new AuthDtos.AuthResponse(user.getId(), user.getFullName(),
                user.getEmail(), hasBusiness, complete);
    }

    private String rateLimitKey(String email, HttpServletRequest request) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return normalized + "|" + ip;
    }

    private static String normalizeMobile(String mobile) {
        return mobile == null ? null : mobile.trim();
    }
}
