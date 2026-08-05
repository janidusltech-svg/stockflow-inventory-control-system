package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "stock_count_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_stock_count_balance",
                columnNames = {"stock_count_id", "inventory_balance_id"}
        )
)
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_balance_id", nullable = false)
    private InventoryBalance inventoryBalance;

    @Column(nullable = false)
    private Integer systemQuantity;

    private Integer countedQuantity;

    @Column(length = 255)
    private String varianceNote;

    public int getVariance() {
        return countedQuantity == null ? 0 : countedQuantity - systemQuantity;
    }

    public Long getId() { return id; }
    public StockCount getStockCount() { return stockCount; }
    public void setStockCount(StockCount stockCount) { this.stockCount = stockCount; }
    public InventoryBalance getInventoryBalance() { return inventoryBalance; }
    public void setInventoryBalance(InventoryBalance inventoryBalance) { this.inventoryBalance = inventoryBalance; }
    public Integer getSystemQuantity() { return systemQuantity; }
    public void setSystemQuantity(Integer systemQuantity) { this.systemQuantity = systemQuantity; }
    public Integer getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(Integer countedQuantity) { this.countedQuantity = countedQuantity; }
    public String getVarianceNote() { return varianceNote; }
    public void setVarianceNote(String varianceNote) {
        this.varianceNote = varianceNote == null || varianceNote.isBlank() ? null : varianceNote.trim();
    }
}
