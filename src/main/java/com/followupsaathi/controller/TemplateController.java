package com.followupsaathi.controller;

import com.followupsaathi.dto.TemplateDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Template CRUD, preview against real or sample data, and share links. */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final CurrentUser currentUser;

    public TemplateController(TemplateService templateService, CurrentUser currentUser) {
        this.templateService = templateService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TemplateDtos.View> list() {
        return templateService.list(currentUser.requireUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDtos.View create(@Valid @RequestBody TemplateDtos.Create request) {
        return templateService.create(currentUser.requireUserId(), request);
    }

    @PutMapping("/{id}")
    public TemplateDtos.View update(@PathVariable Long id,
            @Valid @RequestBody TemplateDtos.Update request) {
        return templateService.update(currentUser.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        templateService.delete(currentUser.requireUserId(), id);
    }

    /** Preview: omit enquiryId for sample-data rendering. */
    @GetMapping("/{id}/preview")
    public TemplateDtos.Preview preview(@PathVariable Long id,
            @RequestParam(name = "enquiryId", required = false) Long enquiryId) {
        return templateService.preview(currentUser.requireUserId(), id, enquiryId);
    }

    /** Returns the rendered message plus the WhatsApp/SMS deep link. */
    @PostMapping("/{id}/share/{enquiryId}")
    public TemplateDtos.Share share(@PathVariable Long id,
            @PathVariable Long enquiryId,
            @RequestParam(name = "channel", defaultValue = "WHATSAPP") String channel) {
        return templateService.share(currentUser.requireUserId(), id, enquiryId, channel);
    }
}
