package com.stockflow.transfer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StockTransferForm {

    @NotNull(message = "Select a source location")
    private Long sourceLocationId;

    @NotNull(message = "Select a destination location")
    private Long destinationLocationId;

    @NotNull(message = "Transfer date is required")
    @PastOrPresent(message = "Transfer date cannot be in the future")
    private LocalDate transferDate = LocalDate.now();

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
