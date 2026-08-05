package com.stockflow.control;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockAdjustmentForm {

    @NotNull(message = "Inventory balance is required")
    private Long inventoryBalanceId;

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    @NotBlank(message = "Adjustment reason is required")
    @Size(max = 150, message = "Reason must be 150 characters or less")
    private String reason;

    @Size(max = 500, message = "Notes must be 500 characters or less")
    private String notes;

    public Long getInventoryBalanceId() { return inventoryBalanceId; }
    public void setInventoryBalanceId(Long inventoryBalanceId) { this.inventoryBalanceId = inventoryBalanceId; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
