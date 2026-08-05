package com.stockflow.control;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReorderRuleForm {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Minimum level is required")
    @Min(value = 0, message = "Minimum level cannot be negative")
    private Integer minimumLevel;

    @NotNull(message = "Maximum level is required")
    @Min(value = 1, message = "Maximum level must be at least one")
    private Integer maximumLevel;

    @NotNull(message = "Reorder quantity is required")
    @Min(value = 1, message = "Reorder quantity must be at least one")
    private Integer reorderQuantity;

    private boolean active = true;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getMinimumLevel() { return minimumLevel; }
    public void setMinimumLevel(Integer minimumLevel) { this.minimumLevel = minimumLevel; }
    public Integer getMaximumLevel() { return maximumLevel; }
    public void setMaximumLevel(Integer maximumLevel) { this.maximumLevel = maximumLevel; }
    public Integer getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(Integer reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
