package com.stockflow.shared.audit;

import com.stockflow.shared.reporting.ChartBar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuditTrailService {

    private static final int PAGE_SIZE = 20;
    private static final int MAX_EXPORT_ROWS = 5000;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditEventRepository auditEventRepository;

    public AuditTrailService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String roleName, String module, String action,
                       AuditOutcome outcome, String httpMethod, String requestPath,
                       String referenceValue, String description, int responseStatus,
                       String ipAddress) {
        AuditEvent event = new AuditEvent(
                safe(username, "unknown", 50),
                safe(roleName, "UNKNOWN", 60),
                safe(module, "Other", 80),
                safe(action, "ACTION", 40),
                outcome,
                safe(httpMethod, "POST", 10),
                safe(requestPath, "/", 255),
                nullable(referenceValue, 100),
                safe(description, "StockFlow data-changing request", 500),
                responseStatus,
                nullable(ipAddress, 64)
        );
        auditEventRepository.save(event);
    }

    public Page<AuditEvent> search(String keyword, String module, String action,
                                   AuditOutcome outcome, int requestedPage) {
        int page = Math.max(0, requestedPage);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<AuditEvent> result = auditEventRepository.search(
                clean(keyword), clean(module), clean(action), outcome, pageable);
        if (result.getTotalPages() > 0 && page >= result.getTotalPages()) {
            return auditEventRepository.search(clean(keyword), clean(module), clean(action), outcome,
                    PageRequest.of(result.getTotalPages() - 1, PAGE_SIZE));
        }
        return result;
    }

    public AuditSummary summary() {
        return new AuditSummary(
                auditEventRepository.count(),
                auditEventRepository.countByOutcome(AuditOutcome.SUCCESS),
                auditEventRepository.countByOutcome(AuditOutcome.FAILURE),
                auditEventRepository.countByOccurredAtAfter(LocalDateTime.now().minusHours(24))
        );
    }

    public List<ChartBar> activityByModule() {
        Map<String, Long> values = eventsLastThirtyDays().stream()
                .collect(Collectors.groupingBy(AuditEvent::getModule,
                        LinkedHashMap::new, Collectors.counting()));
        return toBars(values, 8);
    }

    public List<ChartBar> activityByAction() {
        Map<String, Long> values = eventsLastThirtyDays().stream()
                .collect(Collectors.groupingBy(event -> readable(event.getAction()),
                        LinkedHashMap::new, Collectors.counting()));
        return toBars(values, 8);
    }

    public List<String> modules() {
        return auditEventRepository.findDistinctModules();
    }

    public List<String> actions() {
        return auditEventRepository.findDistinctActions();
    }

    public byte[] exportCsv(String keyword, String module, String action, AuditOutcome outcome) {
        Page<AuditEvent> events = auditEventRepository.search(
                clean(keyword), clean(module), clean(action), outcome,
                PageRequest.of(0, MAX_EXPORT_ROWS));
        StringBuilder csv = new StringBuilder();
        row(csv, "Date and Time", "Username", "Role", "Module", "Action", "Outcome",
                "Reference", "Request Path", "HTTP Status", "IP Address", "Description");
        for (AuditEvent event : events.getContent()) {
            row(csv,
                    DATE_TIME.format(event.getOccurredAt()), event.getUsername(), event.getRoleName(),
                    event.getModule(), readable(event.getAction()), event.getOutcome(),
                    event.getReferenceValue(), event.getRequestPath(), event.getResponseStatus(),
                    event.getIpAddress(), event.getDescription());
        }
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private List<AuditEvent> eventsLastThirtyDays() {
        return auditEventRepository.findByOccurredAtAfterOrderByOccurredAtDesc(LocalDateTime.now().minusDays(30));
    }

    private List<ChartBar> toBars(Map<String, Long> values, int limit) {
        long maximum = values.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> new ChartBar(entry.getKey(), entry.getValue(), percentage(entry.getValue(), maximum)))
                .toList();
    }

    private int percentage(long value, long maximum) {
        if (value <= 0 || maximum <= 0) {
            return 0;
        }
        return Math.max(4, (int) Math.round(value * 100.0 / maximum));
    }

    private void row(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append("\r\n");
    }

    private String escape(Object raw) {
        String value = raw == null ? "" : raw.toString();
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String readable(String value) {
        if (value == null || value.isBlank()) {
            return "Action";
        }
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback, int maximumLength) {
        String cleaned = value == null || value.isBlank() ? fallback : value.trim();
        return cleaned.substring(0, Math.min(cleaned.length(), maximumLength));
    }

    private String nullable(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.substring(0, Math.min(cleaned.length(), maximumLength));
    }
}
