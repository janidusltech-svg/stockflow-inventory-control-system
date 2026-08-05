package com.stockflow.control;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class StockCountForm {

    @NotNull(message = "Storage location is required")
    private Long locationId;

    @NotNull(message = "Count date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate countDate = LocalDate.now();

    @Size(max = 500, message = "Notes must be 500 characters or less")
    private String notes;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public LocalDate getCountDate() { return countDate; }
    public void setCountDate(LocalDate countDate) { this.countDate = countDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
