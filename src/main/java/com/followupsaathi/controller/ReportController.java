package com.followupsaathi.controller;

import com.followupsaathi.dto.ReportDtos;
import com.followupsaathi.security.CurrentUser;
import com.followupsaathi.service.ReportService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Aggregate report numbers for the reports screen. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
            @RequestParam(name = "format", required = false) String format,
            @RequestParam(name = "range", required = false) String range,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        if ("csv".equalsIgnoreCase(format)) {
            String csv = reportService.summaryCsv(currentUser.requireUserId(), range, from, to);
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("followup-report.csv", StandardCharsets.UTF_8).build().toString())
                    .body(csv);
        }
        return ResponseEntity.ok(reportService.summary(currentUser.requireUserId(), range, from, to));
    }
}
