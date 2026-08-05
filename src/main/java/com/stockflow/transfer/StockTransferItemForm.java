package com.stockflow.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockTransferItemForm {

    @NotNull(message = "Select an available stock record")
    private Long inventoryBalanceId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public Long getInventoryBalanceId() {
        return inventoryBalanceId;
    }

    public void setInventoryBalanceId(Long inventoryBalanceId) {
        this.inventoryBalanceId = inventoryBalanceId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
