package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String adjustmentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_balance_id", nullable = false)
    private InventoryBalance inventoryBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id")
    private StockCount stockCount;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false, length = 150)
    private String reason;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.DRAFT;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

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

    public boolean isEditable() { return status == AdjustmentStatus.DRAFT; }
    public boolean isReviewable() { return status == AdjustmentStatus.PENDING; }

    public Long getId() { return id; }
    public String getAdjustmentNumber() { return adjustmentNumber; }
    public void setAdjustmentNumber(String adjustmentNumber) { this.adjustmentNumber = adjustmentNumber; }
    public InventoryBalance getInventoryBalance() { return inventoryBalance; }
    public void setInventoryBalance(InventoryBalance inventoryBalance) { this.inventoryBalance = inventoryBalance; }
    public StockCount getStockCount() { return stockCount; }
    public void setStockCount(StockCount stockCount) { this.stockCount = stockCount; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = clean(reason); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = clean(notes); }
    public AdjustmentStatus getStatus() { return status; }
    public void setStatus(AdjustmentStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
