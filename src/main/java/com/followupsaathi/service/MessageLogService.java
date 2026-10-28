package com.followupsaathi.service;

import com.followupsaathi.dto.MessageLogDtos;
import com.followupsaathi.entity.MessageChannel;
import com.followupsaathi.entity.MessageLog;
import com.followupsaathi.exception.ApiException;
import com.followupsaathi.repository.MessageLogRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Message log: only records messages the owner chose to send by opening the
 * messaging app themselves. Nothing is auto-sent, matching the v1 scope.
 */
@Service
public class MessageLogService {

    private final MessageLogRepository repository;
    private final EnquiryService enquiryService;

    public MessageLogService(MessageLogRepository repository,
            EnquiryService enquiryService) {
        this.repository = repository;
        this.enquiryService = enquiryService;
    }

    @Transactional
    public MessageLogDtos.View record(Long userId, MessageLogDtos.Create request) {
        var enquiry = enquiryService.requireOwnedEnquiry(userId, request.enquiryId());
        MessageChannel channel;
        try {
            channel = MessageChannel.valueOf(request.channel().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Channel must be WHATSAPP or SMS");
        }

        MessageLog log = new MessageLog();
        log.setEnquiry(enquiry);
        log.setChannel(channel);
        log.setMessageBody(request.messageBody().trim());
        log.setSentAt(Instant.now());
        log = repository.save(log);

        return toView(log);
    }

    @Transactional(readOnly = true)
    public Page<MessageLogDtos.View> history(Long userId, Long enquiryId,
            int page, int size) {
        enquiryService.requireOwnedEnquiry(userId, enquiryId);
        int safeSize = Math.max(1, Math.min(size, 50));
        return repository
                .findByEnquiryIdOrderBySentAtDesc(enquiryId,
                        PageRequest.of(Math.max(0, page), safeSize,
                                Sort.by(Sort.Direction.DESC, "sentAt")))
                .map(this::toView);
    }

    private MessageLogDtos.View toView(MessageLog log) {
        return new MessageLogDtos.View(
                log.getId(),
                log.getEnquiry().getId(),
                log.getEnquiry().getCustomerName(),
                log.getChannel().name(),
                log.getMessageBody(),
                log.getSentAt());
    }
}
