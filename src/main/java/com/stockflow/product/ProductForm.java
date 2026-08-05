package com.stockflow.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductForm {

    @NotBlank(message = "SKU is required")
    @Size(max = 40, message = "SKU must be 40 characters or less")
    private String sku;

    @Size(max = 80, message = "Barcode must be 80 characters or less")
    private String barcode;

    @NotBlank(message = "Product name is required")
    @Size(max = 120, message = "Product name must be 120 characters or less")
    private String name;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Long brandId;

    @NotNull(message = "Unit is required")
    private Long unitId;

    @Size(max = 255, message = "Description must be 255 characters or less")
    private String description;

    private boolean batchTracked;
    private boolean active = true;

    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.setSku(product.getSku());
        form.setBarcode(product.getBarcode());
        form.setName(product.getName());
        form.setCategoryId(product.getCategory().getId());
        form.setBrandId(
                product.getBrand() == null ? null : product.getBrand().getId()
        );
        form.setUnitId(product.getUnit().getId());
        form.setDescription(product.getDescription());
        form.setBatchTracked(product.isBatchTracked());
        form.setActive(product.isActive());
        return form;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBatchTracked() {
        return batchTracked;
    }

    public void setBatchTracked(boolean batchTracked) {
        this.batchTracked = batchTracked;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
