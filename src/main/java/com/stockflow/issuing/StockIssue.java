package com.stockflow.issuing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_issues")
public class StockIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String issueNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(length = 80)
    private String externalReference;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StockIssueStatus status = StockIssueStatus.DRAFT;

    @OneToMany(mappedBy = "stockIssue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockIssueItem> items = new ArrayList<>();

    private LocalDateTime confirmedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    public boolean isEditable() {
        return status == StockIssueStatus.DRAFT;
    }

    public boolean isReturnable() {
        return status == StockIssueStatus.CONFIRMED
                || status == StockIssueStatus.PARTIALLY_RETURNED;
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(StockIssueItem::getQuantity).sum();
    }

    public int getTotalReturnedQuantity() {
        return items.stream().mapToInt(StockIssueItem::getReturnedQuantity).sum();
    }

    public Long getId() { return id; }
    public String getIssueNumber() { return issueNumber; }
    public void setIssueNumber(String issueNumber) { this.issueNumber = issueNumber; }
    public Recipient getRecipient() { return recipient; }
    public void setRecipient(Recipient recipient) { this.recipient = recipient; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String value) { externalReference = clean(value); }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = clean(value); }
    public StockIssueStatus getStatus() { return status; }
    public void setStatus(StockIssueStatus status) { this.status = status; }
    public List<StockIssueItem> getItems() { return items; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
