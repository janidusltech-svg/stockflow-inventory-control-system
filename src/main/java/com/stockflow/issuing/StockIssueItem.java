package com.stockflow.issuing;

import com.stockflow.inventory.Batch;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
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
@Table(name = "stock_issue_items")
public class StockIssueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_issue_id", nullable = false)
    private StockIssue stockIssue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private StorageLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer returnedQuantity = 0;

    public int getRemainingReturnableQuantity() {
        return quantity - returnedQuantity;
    }

    public void addReturnedQuantity(int value) {
        if (value <= 0 || value > getRemainingReturnableQuantity()) {
            throw new IllegalArgumentException("Return quantity exceeds the remaining issued quantity");
        }
        returnedQuantity += value;
    }

    public Long getId() { return id; }
    public StockIssue getStockIssue() { return stockIssue; }
    public void setStockIssue(StockIssue stockIssue) { this.stockIssue = stockIssue; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public StorageLocation getLocation() { return location; }
    public void setLocation(StorageLocation location) { this.location = location; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }
}
