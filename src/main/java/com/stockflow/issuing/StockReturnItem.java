package com.stockflow.issuing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_return_items")
public class StockReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_return_id", nullable = false)
    private StockReturn stockReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_item_id", nullable = false)
    private StockIssueItem issueItem;

    @Column(nullable = false)
    private Integer returnedQuantity;

    @Column(nullable = false)
    private Integer restockQuantity;

    @Column(nullable = false)
    private Integer rejectedQuantity;

    @Column(nullable = false, length = 150)
    private String returnReason;

    @Column(length = 255)
    private String conditionNote;

    public Long getId() { return id; }
    public StockReturn getStockReturn() { return stockReturn; }
    public void setStockReturn(StockReturn stockReturn) { this.stockReturn = stockReturn; }
    public StockIssueItem getIssueItem() { return issueItem; }
    public void setIssueItem(StockIssueItem issueItem) { this.issueItem = issueItem; }
    public Integer getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }
    public Integer getRestockQuantity() { return restockQuantity; }
    public void setRestockQuantity(Integer restockQuantity) { this.restockQuantity = restockQuantity; }
    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String value) { returnReason = value == null ? null : value.trim(); }
    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String value) {
        conditionNote = value == null || value.isBlank() ? null : value.trim();
    }
}
