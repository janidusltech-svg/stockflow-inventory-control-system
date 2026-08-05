package com.stockflow.transfer;

import com.stockflow.warehouse.StorageLocation;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_location_id", nullable = false)
    private StorageLocation sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private StorageLocation destinationLocation;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockTransferStatus status = StockTransferStatus.DRAFT;

    @Column(length = 500)
    private String notes;

    @OneToMany(
            mappedBy = "stockTransfer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockTransferItem> items = new ArrayList<>();

    private LocalDateTime dispatchedAt;

    private LocalDateTime receivedAt;

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

    public Long getId() {
        return id;
    }

    public String getTransferNumber() {
        return transferNumber;
    }

    public void setTransferNumber(String transferNumber) {
        this.transferNumber = transferNumber;
    }

    public StorageLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(StorageLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public StorageLocation getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(StorageLocation destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public StockTransferStatus getStatus() {
        return status;
    }

    public void setStatus(StockTransferStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    }

    public List<StockTransferItem> getItems() {
        return items;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(StockTransferItem::getQuantity).sum();
    }

    public boolean isEditable() {
        return status == StockTransferStatus.DRAFT;
    }

    public boolean canApprove() {
        return status == StockTransferStatus.DRAFT && !items.isEmpty();
    }

    public boolean canDispatch() {
        return status == StockTransferStatus.APPROVED;
    }

    public boolean canReceive() {
        return status == StockTransferStatus.DISPATCHED;
    }

    public boolean canCancel() {
        return status == StockTransferStatus.DRAFT
                || status == StockTransferStatus.APPROVED;
    }
}
