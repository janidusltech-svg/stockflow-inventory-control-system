package com.stockflow.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_username", columnList = "username"),
        @Index(name = "idx_audit_module_action", columnList = "module_name,action"),
        @Index(name = "idx_audit_outcome", columnList = "outcome")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "role_name", nullable = false, length = 60)
    private String roleName;

    @Column(name = "module_name", nullable = false, length = 80)
    private String module;

    @Column(nullable = false, length = 40)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 255)
    private String requestPath;

    @Column(name = "reference_value", length = 100)
    private String referenceValue;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String username, String roleName, String module, String action,
                      AuditOutcome outcome, String httpMethod, String requestPath,
                      String referenceValue, String description, int responseStatus,
                      String ipAddress) {
        this.username = username;
        this.roleName = roleName;
        this.module = module;
        this.action = action;
        this.outcome = outcome;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.referenceValue = referenceValue;
        this.description = description;
        this.responseStatus = responseStatus;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRoleName() { return roleName; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getHttpMethod() { return httpMethod; }
    public String getRequestPath() { return requestPath; }
    public String getReferenceValue() { return referenceValue; }
    public String getDescription() { return description; }
    public int getResponseStatus() { return responseStatus; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
