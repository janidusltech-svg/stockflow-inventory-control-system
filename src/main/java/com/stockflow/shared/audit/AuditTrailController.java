package com.stockflow.shared.audit;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/audit-trail")
@PreAuthorize("hasAnyRole('OPERATIONS_MANAGER','IT_SYSTEMS_OFFICER')")
public class AuditTrailController {

    private final AuditTrailService auditTrailService;

    public AuditTrailController(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @GetMapping
    public String auditTrail(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String module,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) AuditOutcome outcome,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        model.addAttribute("auditPage", auditTrailService.search(keyword, module, action, outcome, page));
        model.addAttribute("auditSummary", auditTrailService.summary());
        model.addAttribute("moduleActivity", auditTrailService.activityByModule());
        model.addAttribute("actionActivity", auditTrailService.activityByAction());
        model.addAttribute("modules", auditTrailService.modules());
        model.addAttribute("actions", auditTrailService.actions());
        model.addAttribute("outcomes", AuditOutcome.values());
        model.addAttribute("keyword", clean(keyword));
        model.addAttribute("selectedModule", clean(module));
        model.addAttribute("selectedAction", clean(action));
        model.addAttribute("selectedOutcome", outcome);
        return "audit/index";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String module,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) AuditOutcome outcome) {
        byte[] content = auditTrailService.exportCsv(keyword, module, action, outcome);
        String filename = "stockflow_audit_trail_" + LocalDate.now() + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
