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
@Table(name = "stock_returns")
public class StockReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String returnNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stock_issue_id", nullable = false)
    private StockIssue stockIssue;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockReturnStatus status = StockReturnStatus.DRAFT;

    @OneToMany(mappedBy = "stockReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockReturnItem> items = new ArrayList<>();

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
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean isEditable() { return status == StockReturnStatus.DRAFT; }
    public int getTotalReturnedQuantity() {
        return items.stream().mapToInt(StockReturnItem::getReturnedQuantity).sum();
    }
    public int getTotalRestockedQuantity() {
        return items.stream().mapToInt(StockReturnItem::getRestockQuantity).sum();
    }
    public int getTotalRejectedQuantity() {
        return items.stream().mapToInt(StockReturnItem::getRejectedQuantity).sum();
    }

    public Long getId() { return id; }
    public String getReturnNumber() { return returnNumber; }
    public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }
    public StockIssue getStockIssue() { return stockIssue; }
    public void setStockIssue(StockIssue stockIssue) { this.stockIssue = stockIssue; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = clean(value); }
    public StockReturnStatus getStatus() { return status; }
    public void setStatus(StockReturnStatus status) { this.status = status; }
    public List<StockReturnItem> getItems() { return items; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
