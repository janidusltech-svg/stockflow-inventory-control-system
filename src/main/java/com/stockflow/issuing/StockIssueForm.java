package com.stockflow.issuing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StockIssueForm {

    @NotNull(message = "Select a recipient")
    private Long recipientId;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate = LocalDate.now();

    @Size(max = 80, message = "Reference must be 80 characters or fewer")
    private String externalReference;

    @Size(max = 500, message = "Notes must be 500 characters or fewer")
    private String notes;

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
