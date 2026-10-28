package com.followupsaathi.controller;

import com.followupsaathi.dto.EnquiryDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.EnquiryService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enquiry endpoints. Note the list endpoint shape: the frontend sends
 * ?page=&size=&status=&q=&due= — one URL builder in the API client, no
 * special cases.
 */
@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;
    private final CurrentUser currentUser;

    public EnquiryController(EnquiryService enquiryService, CurrentUser currentUser) {
        this.enquiryService = enquiryService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryDtos.View create(@Valid @RequestBody EnquiryDtos.Create request) {
        return enquiryService.create(currentUser.requireUserId(), request);
    }

    @GetMapping
    public EnquiryDtos.ListResponse list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "archived", required = false) Boolean archived,
            @RequestParam(name = "due", required = false) Boolean due) {
        return enquiryService.list(currentUser.requireUserId(), status, search,
                archived, due, page, size);
    }

    @GetMapping("/{id}")
    public EnquiryDtos.View get(@PathVariable Long id) {
        return enquiryService.get(currentUser.requireUserId(), id);
    }

    @PatchMapping("/{id}")
    public EnquiryDtos.View update(@PathVariable Long id,
            @Valid @RequestBody EnquiryDtos.Update request) {
        return enquiryService.update(currentUser.requireUserId(), id, request);
    }

    @PatchMapping("/{id}/archive")
    public EnquiryDtos.View archive(@PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean archived = Boolean.TRUE.equals(body.get("archived"));
        return enquiryService.archive(currentUser.requireUserId(), id, archived);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enquiryService.delete(currentUser.requireUserId(), id);
    }

    @GetMapping("/{id}/history")
    public java.util.List<EnquiryDtos.Activity> history(@PathVariable Long id) {
        return enquiryService.history(currentUser.requireUserId(), id);
    }
}
