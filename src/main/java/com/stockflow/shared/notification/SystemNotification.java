package com.stockflow.shared.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "system_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_user_source",
                columnNames = {"username", "source_key"}
        )
)
public class SystemNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "source_key", nullable = false, length = 140)
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_url", nullable = false, length = 255)
    private String targetUrl;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SystemNotification() {
    }

    public SystemNotification(String username, String sourceKey, NotificationType type,
                              String title, String message, String targetUrl) {
        this.username = username;
        this.sourceKey = sourceKey;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateContent(NotificationType type, String title, String message, String targetUrl) {
        boolean changed = this.type != type
                || !this.title.equals(title)
                || !this.message.equals(message)
                || !this.targetUrl.equals(targetUrl);
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        if (changed) {
            this.readFlag = false;
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getSourceKey() { return sourceKey; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
