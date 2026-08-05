package com.stockflow.control;

import com.stockflow.warehouse.StorageLocation;
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
@Table(name = "stock_counts")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String countNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @Column(nullable = false)
    private LocalDate countDate;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCountStatus status = StockCountStatus.DRAFT;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCountItem> items = new ArrayList<>();

    private LocalDateTime submittedAt;
    private LocalDateTime closedAt;

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

    public boolean isEditable() { return status == StockCountStatus.DRAFT; }
    public boolean isSubmitted() { return status == StockCountStatus.SUBMITTED; }
    public int getCountedItemCount() {
        return (int) items.stream().filter(item -> item.getCountedQuantity() != null).count();
    }
    public int getVarianceItemCount() {
        return (int) items.stream().filter(item -> item.getCountedQuantity() != null && item.getVariance() != 0).count();
    }

    public Long getId() { return id; }
    public String getCountNumber() { return countNumber; }
    public void setCountNumber(String countNumber) { this.countNumber = countNumber; }
    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }
    public LocalDate getCountDate() { return countDate; }
    public void setCountDate(LocalDate countDate) { this.countDate = countDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null || notes.isBlank() ? null : notes.trim(); }
    public StockCountStatus getStatus() { return status; }
    public void setStatus(StockCountStatus status) { this.status = status; }
    public List<StockCountItem> getItems() { return items; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
