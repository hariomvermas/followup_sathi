package com.followupsaathi.mapper;

import com.followupsaathi.dto.BusinessDtos;
import com.followupsaathi.entity.Business;
import com.followupsaathi.entity.BusinessSettings;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/** Converts between Business/BusinessSettings entities and their DTOs. */
@Component
public class BusinessMapper {

    public Business toEntity(BusinessDtos.Upsert request) {
        Business business = new Business();
        apply(business, request);
        return business;
    }

    public void apply(Business business, BusinessDtos.Upsert request) {
        business.setBusinessName(request.businessName().trim());
        business.setBusinessCategory(request.businessCategory().trim());
        business.setTimeZone(timeZoneOrDefault(request.timeZone()));
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            business.setPreferredLanguage(request.preferredLanguage().trim());
        }
        business.setWorkingDays(joinWorkingDays(request.workingDays()));
    }

    /** Creates the settings row with the values chosen during onboarding. */
    public BusinessSettings toSettings(Business business, BusinessDtos.Upsert request) {
        BusinessSettings settings = new BusinessSettings();
        settings.setBusiness(business);
        applySettings(settings, request);
        return settings;
    }

    public void applySettings(BusinessSettings settings, BusinessDtos.Upsert request) {
        settings.setReminderEnabled(Boolean.TRUE.equals(request.reminderEnabled()));
        settings.setReminderTime(parseTime(request.reminderTime(), LocalTime.of(10, 0)));
    }

    public BusinessDtos.View toView(Business business, BusinessSettings settings,
            String ownerMobile) {
        boolean complete = notBlank(business.getBusinessName())
                && notBlank(business.getBusinessCategory());
        return new BusinessDtos.View(
                business.getId(),
                business.getBusinessName(),
                business.getBusinessCategory(),
                business.getPreferredLanguage(),
                business.getTimeZone(),
                splitWorkingDays(business.getWorkingDays()),
                settings == null || settings.isReminderEnabled(),
                settings == null || settings.getReminderTime() == null
                        ? "10:00" : settings.getReminderTime().toString(),
                ownerMobile,
                complete,
                business.getOwner().getCreatedAt());
    }

    static List<Integer> splitWorkingDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of(1, 2, 3, 4, 5, 6);
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .map(Integer::valueOf)
                .filter(d -> d >= 1 && d <= 7)
                .distinct()
                .sorted()
                .toList();
    }

    static String joinWorkingDays(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            return "1,2,3,4,5,6";
        }
        return days.stream()
                .filter(d -> d >= 1 && d <= 7)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("1,2,3,4,5,6");
    }

    static String timeZoneOrDefault(String timeZone) {
        return timeZone == null || timeZone.isBlank()
                ? "Asia/Kolkata" : timeZone.trim();
    }

    static LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
