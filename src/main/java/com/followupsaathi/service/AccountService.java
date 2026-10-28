package com.followupsaathi.service;

import com.followupsaathi.dto.AccountDtos;
import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.repository.BusinessRepository;
import com.followupsaathi.repository.BusinessSettingsRepository;
import com.followupsaathi.repository.EnquiryActivityRepository;
import com.followupsaathi.repository.EnquiryRepository;
import com.followupsaathi.repository.FollowUpRepository;
import com.followupsaathi.repository.MessageLogRepository;
import com.followupsaathi.repository.MessageTemplateRepository;
import com.followupsaathi.repository.UserRepository;
import com.followupsaathi.repository.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account management: password change and hard account deletion (spec
 * section 4.12). Deletion removes the business and every dependent row in
 * FK-safe order inside one transaction, then expires the session cookie.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final EnquiryRepository enquiryRepository;
    private final FollowUpRepository followUpRepository;
    private final EnquiryActivityRepository activityRepository;
    private final MessageLogRepository messageLogRepository;
    private final MessageTemplateRepository templateRepository;
    private final UserSessionRepository sessionRepository;
    private final BusinessSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
            BusinessRepository businessRepository,
            EnquiryRepository enquiryRepository,
            FollowUpRepository followUpRepository,
            EnquiryActivityRepository activityRepository,
            MessageLogRepository messageLogRepository,
            MessageTemplateRepository templateRepository,
            UserSessionRepository sessionRepository,
            BusinessSettingsRepository settingsRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.enquiryRepository = enquiryRepository;
        this.followUpRepository = followUpRepository;
        this.activityRepository = activityRepository;
        this.messageLogRepository = messageLogRepository;
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Changes the password of the logged-in owner; other sessions stay valid. */
    @Transactional
    public void changePassword(Long userId, AccountDtos.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(),
                        user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest(
                    "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Hard-deletes the account and every owned row; cannot be undone. */
    @Transactional
    public void deleteAccount(Long userId, AccountDtos.DeleteAccountRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Password is incorrect");
        }

        var business = businessRepository.findByOwnerId(userId).orElse(null);
        if (business != null) {
            Long businessId = business.getId();
            followUpRepository.deleteByEnquiryBusinessId(businessId);
            messageLogRepository.deleteByEnquiryBusinessId(businessId);
            activityRepository.deleteByEnquiryBusinessId(businessId);
            enquiryRepository.deleteByBusinessId(businessId);
            templateRepository.deleteByBusinessId(businessId);
            settingsRepository.deleteByBusinessId(businessId);
            businessRepository.delete(business);
        }
        sessionRepository.deleteByUserId(userId);
        userRepository.delete(user);

        // The server-side session rows are gone; also expire the cookies so
        // the browser stops sending a dead session token.
        expireCookie(httpResponse, "FUSAHI_SESSION");
        expireCookie(httpResponse, "XSRF-TOKEN");
    }

    private static void expireCookie(jakarta.servlet.http.HttpServletResponse response,
            String name) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
        cookie.setPath("/");
        cookie.setHttpOnly("XSRF-TOKEN".equals(name) ? false : true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
