package com.followupsaathi.service;

import com.followupsaathi.dto.BusinessDtos;
import com.followupsaathi.entity.Business;
import com.followupsaathi.entity.BusinessSettings;
import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.mapper.BusinessMapper;
import com.followupsaathi.repository.BusinessRepository;
import com.followupsaathi.repository.BusinessSettingsRepository;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business profile and settings: created during onboarding, edited later. */
@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessSettingsRepository settingsRepository;
    private final BusinessMapper mapper;
    private final CurrentUserProvider users;

    public BusinessService(BusinessRepository businessRepository,
            BusinessSettingsRepository settingsRepository,
            BusinessMapper mapper, CurrentUserProvider users) {
        this.businessRepository = businessRepository;
        this.settingsRepository = settingsRepository;
        this.mapper = mapper;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public BusinessDtos.View view(Long userId) {
        Business business = requireBusiness(userId);
        BusinessSettings settings = settingsRepository
                .findByBusinessId(business.getId()).orElse(null);
        return mapper.toView(business, settings,
                business.getOwner() != null ? business.getOwner().getMobile() : null);
    }

    /** Creates or updates the profile and its settings in one transaction. */
    @Transactional
    public BusinessDtos.View upsert(Long userId, BusinessDtos.Upsert request) {
        validateTimeZone(request.timeZone());
        User user = users.requireUser(userId);
        Business business = businessRepository.findByOwnerId(userId)
                .orElseGet(() -> {
                    Business created = new Business();
                    created.setOwner(user);
                    return created;
                });

        mapper.apply(business, request);
        Business savedBusiness = businessRepository.save(business);

        BusinessSettings settings = settingsRepository
                .findByBusinessId(savedBusiness.getId())
                .orElseGet(() -> mapper.toSettings(savedBusiness, request));
        mapper.applySettings(settings, request);
        settingsRepository.save(settings);

        return mapper.toView(savedBusiness, settings, user.getMobile());
    }

    /** Returns the business for a user or throws 404. */
    public Business requireBusiness(Long userId) {
        return businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> ApiException.notFound(
                        "Complete your business profile first"));
    }

    /** Zone used for every "today" calculation of this business. */
    public ZoneId zone(Long userId) {
        return requireBusiness(userId).zoneId();
    }

    private void validateTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return; // default applies
        }
        try {
            ZoneId.of(timeZone.trim());
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Unknown time zone: " + timeZone);
        }
    }

    /** Minimal indirection so tests can stub user lookup. */
    public interface CurrentUserProvider {
        com.followupsaathi.entity.User requireUser(Long userId);
    }
}
