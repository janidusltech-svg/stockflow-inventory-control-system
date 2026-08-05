package com.stockflow.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StorageLocationForm {

    @NotNull(message = "Select a warehouse")
    private Long warehouseId;

    @NotBlank(message = "Location code is required")
    @Size(max = 30, message = "Location code must not exceed 30 characters")
    @Pattern(
            regexp = "[A-Za-z0-9-]+",
            message = "Location code can contain only letters, numbers and hyphens"
    )
    private String code;

    @NotBlank(message = "Location name is required")
    @Size(max = 100, message = "Location name must not exceed 100 characters")
    private String name;

    private boolean active = true;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
