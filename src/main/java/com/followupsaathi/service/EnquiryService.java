package com.followupsaathi.service;

import com.followupsaathi.dto.EnquiryDtos;
import com.followupsaathi.entity.Enquiry;
import com.followupsaathi.entity.EnquiryActivity;
import com.followupsaathi.entity.EnquiryPriority;
import com.followupsaathi.entity.EnquirySource;
import com.followupsaathi.entity.EnquiryStatus;
import com.followupsaathi.entity.FollowUp;
import com.followupsaathi.entity.FollowUpStatus;
import com.followupsaathi.entity.User;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.mapper.EnquiryMapper;
import com.followupsaathi.repository.EnquiryActivityRepository;
import com.followupsaathi.repository.EnquiryRepository;
import com.followupsaathi.repository.FollowUpRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Enquiry CRUD and search. Every query is scoped by business id; the API
 * never trusts a client-supplied business reference. All list/history actions
 * record activity rows describing exactly what changed.
 */
@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EnquiryActivityRepository activityRepository;
    private final FollowUpRepository followUpRepository;
    private final EnquiryMapper mapper;
    private final BusinessService businessService;
    private final com.followupsaathi.repository.BusinessRepository businessRepository;
    private final com.followupsaathi.repository.UserRepository userRepository;

    public EnquiryService(EnquiryRepository enquiryRepository,
            EnquiryActivityRepository activityRepository,
            FollowUpRepository followUpRepository,
            EnquiryMapper mapper,
            BusinessService businessService,
            com.followupsaathi.repository.BusinessRepository businessRepository,
            com.followupsaathi.repository.UserRepository userRepository) {
        this.enquiryRepository = enquiryRepository;
        this.activityRepository = activityRepository;
        this.followUpRepository = followUpRepository;
        this.mapper = mapper;
        this.businessService = businessService;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnquiryDtos.View create(Long userId, EnquiryDtos.Create request) {
        var business = businessService.requireBusiness(userId);
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));

        Enquiry enquiry = mapper.toEntity(request);
        enquiry.setBusiness(business);
        enquiry.setStatus(EnquiryStatus.NEW);
        enquiry = enquiryRepository.save(enquiry);

        activityRepository.save(EnquiryActivity.of(enquiry, actor,
                com.followupsaathi.entity.ActivityEventType.ENQUIRY_CREATED,
                "Enquiry created for " + enquiry.getCustomerName()));

        return mapper.toView(enquiry, pendingCount(enquiry));
    }

    /** Filtered, paginated list. Only matching rows are returned. */
    @Transactional(readOnly = true)
    public EnquiryDtos.ListResponse list(Long userId, String status, String search,
            Boolean archived, Boolean due, Integer page, Integer size) {
        Long businessId = businessService.requireBusiness(userId).getId();
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1
                ? 10 : Math.min(size, com.followupsaathi.config.AppConstants.MAX_PAGE_SIZE);

        Specification<Enquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("business").get("id"), businessId));
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"),
                        EnquiryStatus.valueOf(status.trim().toUpperCase())));
            }
            if (archived != null) {
                predicates.add(cb.equal(root.get("archived"), archived));
            }
            if (due != null && due) {
                ZoneId zone = businessService.zone(userId);
                predicates.add(cb.equal(root.get("nextFollowUpDate"),
                        LocalDate.now(zone)));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("phoneNumber")), like),
                        cb.like(cb.lower(root.get("serviceName")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Enquiry> result = enquiryRepository.findAll(spec,
                PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        List<EnquiryDtos.ListItem> items = result.getContent().stream()
                .map(e -> mapper.toListItem(e, pendingCount(e) > 0))
                .toList();

        return new EnquiryDtos.ListResponse(items, safePage, safeSize,
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public EnquiryDtos.View get(Long userId, Long enquiryId) {
        Enquiry enquiry = requireOwnedEnquiry(userId, enquiryId);
        return mapper.toView(enquiry, pendingCount(enquiry));
    }

    @Transactional
    public EnquiryDtos.View update(Long userId, Long enquiryId,
            EnquiryDtos.Update request) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        Enquiry enquiry = requireOwnedEnquiry(userId, enquiryId);

        List<String> changes = new ArrayList<>();
        if (request.customerName() != null
                && !request.customerName().equals(enquiry.getCustomerName())) {
            changes.add("name");
        }
        if (request.status() != null
                && !request.status().equals(enquiry.getStatus().name())) {
            changes.add("status");
        }
        EnquiryStatus previousStatus = enquiry.getStatus();

        mapper.applyUpdate(enquiry, request);
        Enquiry saved = enquiryRepository.save(enquiry);

        if (changes.contains("status")) {
            activityRepository.save(EnquiryActivity.of(saved, actor,
                    com.followupsaathi.entity.ActivityEventType.STATUS_CHANGED,
                    "Status changed from " + previousStatus + " to "
                            + saved.getStatus()));
            if (saved.getStatus() == EnquiryStatus.CONVERTED
                    && previousStatus != EnquiryStatus.CONVERTED) {
                saved.markConverted(java.time.Instant.now());
                activityRepository.save(EnquiryActivity.of(saved, actor,
                        com.followupsaathi.entity.ActivityEventType.ENQUIRY_CONVERTED,
                        "Enquiry converted"));
            }
        }
        if (!changes.isEmpty() && !changes.contains("status")) {
            activityRepository.save(EnquiryActivity.of(saved, actor,
                    com.followupsaathi.entity.ActivityEventType.ENQUIRY_UPDATED,
                    "Details updated (" + String.join(", ", changes) + ")"));
        }

        return mapper.toView(saved, pendingCount(saved));
    }

    /** Soft-archive; history stays intact. */
    @Transactional
    public EnquiryDtos.View archive(Long userId, Long enquiryId, boolean archived) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        Enquiry enquiry = requireOwnedEnquiry(userId, enquiryId);
        enquiry.setArchived(archived);
        Enquiry saved = enquiryRepository.save(enquiry);
        activityRepository.save(EnquiryActivity.of(saved, actor,
                archived
                        ? com.followupsaathi.entity.ActivityEventType.ENQUIRY_ARCHIVED
                        : com.followupsaathi.entity.ActivityEventType.ENQUIRY_REOPENED,
                archived ? "Enquiry archived" : "Enquiry reopened"));
        return mapper.toView(saved, pendingCount(saved));
    }

    /** Physical delete, allowed only for enquiries with no follow-ups. */
    @Transactional
    public void delete(Long userId, Long enquiryId) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Not logged in"));
        Enquiry enquiry = requireOwnedEnquiry(userId, enquiryId);
        if (followUpRepository.findByEnquiryIdOrderByScheduledDateDesc(enquiryId)
                .stream().anyMatch(f -> f.getStatus() == FollowUpStatus.PENDING)) {
            throw ApiException.badRequest(
                    "Complete or cancel pending follow-ups before deleting");
        }
        followUpRepository.deleteByEnquiryId(enquiryId);
        activityRepository.findByEnquiryIdOrderByCreatedAtDesc(enquiryId, Pageable.unpaged())
                .forEach(activityRepository::delete);
        enquiryRepository.delete(enquiry);
    }

    @Transactional(readOnly = true)
    public List<EnquiryDtos.Activity> history(Long userId, Long enquiryId) {
        requireOwnedEnquiry(userId, enquiryId);
        return activityRepository
                .findTop50ByEnquiryIdOrderByCreatedAtDesc(enquiryId).stream()
                .map(a -> new EnquiryDtos.Activity(a.getId(), a.getEventType().name(),
                        a.getDescription(),
                        a.getActor() != null ? a.getActor().getFullName() : null,
                        a.getCreatedAt()))
                .toList();
    }

    /** Enforces business ownership; throws 404 (not 403) to avoid probing. */
    public Enquiry requireOwnedEnquiry(Long userId, Long enquiryId) {
        var business = businessService.requireBusiness(userId);
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> ApiException.notFound("Enquiry not found"));
        if (!enquiry.getBusiness().getId().equals(business.getId())) {
            throw ApiException.notFound("Enquiry not found");
        }
        return enquiry;
    }

    private Integer pendingCount(Enquiry enquiry) {
        return (int) followUpRepository
                .findByEnquiryIdOrderByScheduledDateDesc(enquiry.getId()).stream()
                .filter(f -> f.getStatus() == FollowUpStatus.PENDING)
                .count();
    }

    // Suppress unused warnings for the enum imports kept for API stability.
    @SuppressWarnings("unused")
    private static Class<?>[] keptImports() {
        return new Class<?>[] {EnquirySource.class, EnquiryPriority.class,
                EnquiryStatus.class, StringUtils.class};
    }
}
