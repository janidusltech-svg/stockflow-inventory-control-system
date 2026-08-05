package com.stockflow.receiving;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class GoodsReceiptForm {

    @NotNull(message = "Approved purchase order is required")
    private Long purchaseOrderId;

    @NotNull(message = "Receiving location is required")
    private Long locationId;

    @NotNull(message = "Received date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate receivedDate = LocalDate.now();

    @Size(max = 80, message = "Delivery reference must be 80 characters or less")
    private String supplierDeliveryReference;

    @Size(max = 500, message = "Notes must be 500 characters or less")
    private String notes;

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getSupplierDeliveryReference() {
        return supplierDeliveryReference;
    }

    public void setSupplierDeliveryReference(String supplierDeliveryReference) {
        this.supplierDeliveryReference = supplierDeliveryReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
