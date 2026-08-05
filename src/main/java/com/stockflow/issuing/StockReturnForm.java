package com.stockflow.issuing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StockReturnForm {

    @NotNull(message = "Select a confirmed stock issue")
    private Long stockIssueId;

    @NotNull(message = "Return date is required")
    private LocalDate returnDate = LocalDate.now();

    @Size(max = 500, message = "Notes must be 500 characters or fewer")
    private String notes;

    public Long getStockIssueId() { return stockIssueId; }
    public void setStockIssueId(Long stockIssueId) { this.stockIssueId = stockIssueId; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
