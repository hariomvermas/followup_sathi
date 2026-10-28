package com.followupsaathi.service;

import com.followupsaathi.dto.TemplateDtos;
import com.followupsaathi.entity.Business;
import com.followupsaathi.entity.Enquiry;
import com.followupsaathi.entity.MessageChannel;
import com.followupsaathi.entity.MessageTemplate;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.repository.MessageTemplateRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Template CRUD plus placeholder rendering. Placeholders are {{name}} tokens;
 * supported: customerName, businessName, serviceName, appointmentDate,
 * ownerName, phoneNumber. Rendering is simple string replacement performed
 * server-side so previews and shared messages always match.
 */
@Service
public class TemplateService {

    static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z]+)\\s*}}");

    private final MessageTemplateRepository repository;
    private final BusinessService businessService;
    private final EnquiryService enquiryService;
    private final com.followupsaathi.repository.FollowUpRepository followUpRepository;

    public TemplateService(MessageTemplateRepository repository,
            BusinessService businessService,
            EnquiryService enquiryService,
            com.followupsaathi.repository.FollowUpRepository followUpRepository) {
        this.repository = repository;
        this.businessService = businessService;
        this.enquiryService = enquiryService;
        this.followUpRepository = followUpRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateDtos.View> list(Long userId) {
        Long businessId = businessService.requireBusiness(userId).getId();
        return repository.findByBusinessIdOrderByIdAsc(businessId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public TemplateDtos.View create(Long userId, TemplateDtos.Create request) {
        Business business = businessService.requireBusiness(userId);
        if (repository.existsByBusinessIdAndTemplateNameIgnoreCase(
                business.getId(), request.templateName().trim())) {
            throw ApiException.conflict("A template with this name already exists");
        }
        MessageTemplate template = new MessageTemplate();
        template.setBusiness(business);
        template.setTemplateName(request.templateName().trim());
        template.setMessageBody(request.messageBody().trim());
        template.setDefault(false);
        return toView(repository.save(template));
    }

    @Transactional
    public TemplateDtos.View update(Long userId, Long templateId,
            TemplateDtos.Update request) {
        MessageTemplate template = requireOwnedTemplate(userId, templateId);
        if (request.templateName() != null && !request.templateName().isBlank()) {
            String newName = request.templateName().trim();
            if (!newName.equalsIgnoreCase(template.getTemplateName())
                    && repository.existsByBusinessIdAndTemplateNameIgnoreCase(
                            template.getBusiness().getId(), newName)) {
                throw ApiException.conflict("A template with this name already exists");
            }
            template.setTemplateName(newName);
        }
        if (request.messageBody() != null && !request.messageBody().isBlank()) {
            template.setMessageBody(request.messageBody().trim());
        }
        return toView(repository.save(template));
    }

    @Transactional
    public void delete(Long userId, Long templateId) {
        repository.delete(requireOwnedTemplate(userId, templateId));
    }

    /**
     * Renders the template for one enquiry. Missing placeholder values are
     * reported back so the UI can ask the user to fill them instead of
     * silently sending "Hi , ...".
     */
    @Transactional(readOnly = true)
    public TemplateDtos.Preview preview(Long userId, Long templateId, Long enquiryId) {
        MessageTemplate template = requireOwnedTemplate(userId, templateId);
        var values = enquiryId == null
                ? sampleValues()
                : actualValues(enquiryService.requireOwnedEnquiry(userId, enquiryId));
        return render(template.getMessageBody(), values);
    }

    /**
     * Builds the final message and the WhatsApp/SMS deep link for one enquiry.
     * The owner still sends it themselves by opening the messaging app.
     */
    @Transactional
    public TemplateDtos.Share share(Long userId, Long templateId, Long enquiryId,
            String channel) {
        MessageTemplate template = requireOwnedTemplate(userId, templateId);
        Enquiry enquiry = enquiryService.requireOwnedEnquiry(userId, enquiryId);

        TemplateDtos.Preview rendered =
                render(template.getMessageBody(), actualValues(enquiry));
        if (!rendered.missingPlaceholders().isEmpty()) {
            throw ApiException.badRequest("Template uses placeholders with no value: "
                    + String.join(", ", rendered.missingPlaceholders()));
        }

        MessageChannel resolved = resolveChannel(channel);
        String digits = enquiry.getPhoneNumber().replaceAll("[^0-9]", "");
        String waNumber = digits.length() == 10 ? "91" + digits : digits;
        String encoded = java.net.URLEncoder.encode(rendered.rendered(),
                java.nio.charset.StandardCharsets.UTF_8);
        String deepLink = resolved == MessageChannel.WHATSAPP
                ? "https://wa.me/" + waNumber + "?text=" + encoded
                : "sms:" + enquiry.getPhoneNumber() + "?body=" + encoded;

        return new TemplateDtos.Share(enquiryId, resolved.name(),
                rendered.rendered(), deepLink);
    }

    static MessageChannel resolveChannel(String channel) {
        if (!StringUtils.hasText(channel)) {
            return MessageChannel.WHATSAPP;
        }
        try {
            return MessageChannel.valueOf(channel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Channel must be WHATSAPP or SMS");
        }
    }

    private TemplateDtos.Preview render(String body,
            java.util.Map<String, String> values) {
        Set<String> missing = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(body);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
                matcher.appendReplacement(rendered, Matcher.quoteReplacement("{{" + key + "}}"));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(rendered);
        return new TemplateDtos.Preview(rendered.toString(), List.copyOf(missing));
    }

    private java.util.Map<String, String> actualValues(Enquiry enquiry) {
        Business business = enquiry.getBusiness();
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("customerName", enquiry.getCustomerName());
        values.put("businessName", business != null ? business.getBusinessName() : null);
        values.put("serviceName", enquiry.getServiceName());
        values.put("phoneNumber", enquiry.getPhoneNumber());
        values.put("ownerName",
                business != null && business.getOwner() != null
                        ? business.getOwner().getFullName() : null);

        // appointmentDate = the enquiry's next pending follow-up date.
        String appointmentDate = followUpRepository
                .findFirstByEnquiryIdAndStatusOrderByScheduledDateAsc(
                        enquiry.getId(), com.followupsaathi.entity.FollowUpStatus.PENDING)
                .map(f -> f.getScheduledDate()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .orElse(null);
        values.put("appointmentDate", appointmentDate);
        return values;
    }

    private java.util.Map<String, String> sampleValues() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("customerName", "Rahul Sharma");
        values.put("businessName", "Sample Business");
        values.put("serviceName", "Premium Service");
        values.put("phoneNumber", "9876543210");
        values.put("appointmentDate", LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        return values;
    }

    private MessageTemplate requireOwnedTemplate(Long userId, Long templateId) {
        Long businessId = businessService.requireBusiness(userId).getId();
        return repository.findByIdAndBusinessId(templateId, businessId)
                .orElseThrow(() -> ApiException.notFound("Template not found"));
    }

    private TemplateDtos.View toView(MessageTemplate template) {
        return new TemplateDtos.View(
                template.getId(),
                template.getTemplateName(),
                template.getMessageBody(),
                template.isDefault(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    /** Kept for API stability; unused placeholder helper. */
    @SuppressWarnings("unused")
    private static List<String> knownPlaceholders() {
        return List.of("customerName", "businessName", "serviceName",
                "appointmentDate", "ownerName", "phoneNumber");
    }
}
