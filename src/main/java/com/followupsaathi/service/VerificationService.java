package com.followupsaathi.service;

import com.followupsaathi.dto.VerificationDtos;
import com.followupsaathi.entity.MobileVerification;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.exception.ValidationException;
import com.followupsaathi.repository.MobileVerificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mobile verification without any SMS provider: the backend generates a
 * six-digit code and returns ready-to-open WhatsApp/SMS deep links whose
 * prefilled message contains the code. The owner sends it to themselves and
 * confirms it in the UI. v1 keeps codes in the database only; nothing is
 * sent automatically.
 */
@Service
public class VerificationService {

    private final MobileVerificationRepository repository;
    private final com.followupsaathi.repository.UserRepository userRepository;
    private final int codeValidMinutes;
    private final int resendCooldownSeconds;

    public VerificationService(MobileVerificationRepository repository,
            com.followupsaathi.repository.UserRepository userRepository,
            @Value("${app.verification.code-valid-minutes:10}") int codeValidMinutes,
            @Value("${app.verification.resend-cooldown-seconds:60}") int resendCooldownSeconds) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.codeValidMinutes = codeValidMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    @Transactional
    public VerificationDtos.SendResponse send(Long userId, String rawPhone) {
        String phone = normalize(rawPhone);
        if (phone == null || phone.length() < 10) {
            throw new ValidationException("phoneNumber", "Enter a valid mobile number");
        }

        // Cooldown check: the last code must be old enough.
        repository.findFirstByPhoneNumberOrderByCreatedAtDesc(phone).ifPresent(last -> {
            if (last.getCreatedAt()
                    .isAfter(Instant.now().minusSeconds(resendCooldownSeconds))) {
                throw ApiException.tooManyRequests(
                        "Please wait before requesting another code");
            }
        });

        // One active code per number: older rows are superseded.
        repository.deleteByPhoneNumber(phone);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(codeValidMinutes));

        MobileVerification verification = new MobileVerification();
        verification.setPhoneNumber(phone);
        verification.setCode(code);
        verification.setExpiresAt(expiresAt);
        repository.save(verification);

        String message = "Your FollowUp Saathi verification code is: " + code;
        String digits = phone.replaceAll("[^0-9]", "");
        String waNumber = digits.length() == 10 ? "91" + digits : digits;

        return new VerificationDtos.SendResponse(
                mask(phone),
                code,
                "https://wa.me/" + waNumber + "?text="
                        + java.net.URLEncoder.encode(message,
                                java.nio.charset.StandardCharsets.UTF_8),
                "sms:" + phone + "?body="
                        + java.net.URLEncoder.encode(message,
                                java.nio.charset.StandardCharsets.UTF_8),
                expiresAt);
    }

    /** Marks the newest matching code as used; single attempt per code. */
    @Transactional
    public VerificationDtos.VerifyResponse verify(Long userId, String rawPhone,
            String code) {
        String phone = normalize(rawPhone);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));

        MobileVerification verification = repository
                .findFirstByPhoneNumberOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> ApiException.badRequest(
                        "Request a code first, then enter it here"));

        if (verification.isUsed()) {
            throw ApiException.badRequest("This code was already used. Request a new one.");
        }
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This code has expired. Request a new one.");
        }
        if (!verification.getCode().equals(code == null ? "" : code.trim())) {
            throw ApiException.badRequest("Incorrect code. Please check and try again.");
        }

        verification.setUsed(true);
        repository.save(verification);
        user.setMobile(phone);
        userRepository.save(user);

        return new VerificationDtos.VerifyResponse(true,
                "Mobile number verified");
    }

    static String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[\\s\\-()]", "").trim();
    }

    static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4).replaceAll("[0-9]", "*")
                + phone.substring(phone.length() - 4);
    }
}
