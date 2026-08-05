package com.stockflow.issuing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockReturnItemForm {

    @NotNull(message = "Select an issued product")
    private Long issueItemId;

    @NotNull(message = "Returned quantity is required")
    @Min(value = 1, message = "Returned quantity must be at least 1")
    private Integer returnedQuantity;

    @NotNull(message = "Restock quantity is required")
    @Min(value = 0, message = "Restock quantity cannot be negative")
    private Integer restockQuantity = 0;

    @NotNull(message = "Rejected quantity is required")
    @Min(value = 0, message = "Rejected quantity cannot be negative")
    private Integer rejectedQuantity = 0;

    @NotBlank(message = "Return reason is required")
    @Size(max = 150, message = "Return reason must be 150 characters or fewer")
    private String returnReason;

    @Size(max = 255, message = "Condition note must be 255 characters or fewer")
    private String conditionNote;

    public Long getIssueItemId() { return issueItemId; }
    public void setIssueItemId(Long issueItemId) { this.issueItemId = issueItemId; }
    public Integer getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }
    public Integer getRestockQuantity() { return restockQuantity; }
    public void setRestockQuantity(Integer restockQuantity) { this.restockQuantity = restockQuantity; }
    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String conditionNote) { this.conditionNote = conditionNote; }
}
