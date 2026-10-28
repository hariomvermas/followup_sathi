package com.followupsaathi.mapper;

import com.followupsaathi.dto.EnquiryDtos;
import com.followupsaathi.entity.Enquiry;
import com.followupsaathi.entity.EnquirySource;
import com.followupsaathi.entity.EnquiryStatus;
import com.followupsaathi.entity.EnquiryPriority;
import org.springframework.stereotype.Component;

/** Converts between Enquiry entities and their DTOs. */
@Component
public class EnquiryMapper {

    public Enquiry toEntity(EnquiryDtos.Create request) {
        Enquiry enquiry = new Enquiry();
        enquiry.setCustomerName(request.customerName().trim());
        enquiry.setPhoneNumber(trimOrNull(request.phoneNumber()));
        enquiry.setEmail(trimOrNull(request.email()));
        enquiry.setServiceName(trimOrNull(request.serviceName()));
        enquiry.setSource(EnquirySource.valueOf(request.source()));
        enquiry.setPriority(EnquiryPriority.valueOf(request.priority()));
        enquiry.setNotes(trimOrNull(request.notes()));
        return enquiry;
    }

    /** Applies only the fields the client actually sent (null-safe patch). */
    public void applyUpdate(Enquiry enquiry, EnquiryDtos.Update request) {
        if (request.customerName() != null) {
            enquiry.setCustomerName(request.customerName().trim());
        }
        if (request.phoneNumber() != null) {
            enquiry.setPhoneNumber(trimOrNull(request.phoneNumber()));
        }
        if (request.email() != null) {
            enquiry.setEmail(trimOrNull(request.email()));
        }
        if (request.serviceName() != null) {
            enquiry.setServiceName(trimOrNull(request.serviceName()));
        }
        if (request.source() != null) {
            enquiry.setSource(EnquirySource.valueOf(request.source()));
        }
        if (request.status() != null) {
            enquiry.setStatus(EnquiryStatus.valueOf(request.status()));
        }
        if (request.priority() != null) {
            enquiry.setPriority(EnquiryPriority.valueOf(request.priority()));
        }
        if (request.notes() != null) {
            enquiry.setNotes(trimOrNull(request.notes()));
        }
    }

    public EnquiryDtos.View toView(Enquiry enquiry, Integer pendingFollowUps) {
        return new EnquiryDtos.View(
                enquiry.getId(),
                enquiry.getCustomerName(),
                enquiry.getPhoneNumber(),
                enquiry.getEmail(),
                enquiry.getServiceName(),
                enquiry.getSource().name(),
                enquiry.getStatus().name(),
                enquiry.getPriority().name(),
                enquiry.getNextFollowUpDate(),
                enquiry.getNotes(),
                enquiry.isArchived(),
                enquiry.getConvertedAt(),
                enquiry.getCreatedAt(),
                enquiry.getUpdatedAt(),
                pendingFollowUps);
    }

    public EnquiryDtos.ListItem toListItem(Enquiry enquiry, boolean hasPending) {
        return new EnquiryDtos.ListItem(
                enquiry.getId(),
                enquiry.getCustomerName(),
                enquiry.getPhoneNumber(),
                enquiry.getServiceName(),
                enquiry.getStatus().name(),
                enquiry.getPriority().name(),
                enquiry.getNextFollowUpDate(),
                hasPending,
                enquiry.getCreatedAt());
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
