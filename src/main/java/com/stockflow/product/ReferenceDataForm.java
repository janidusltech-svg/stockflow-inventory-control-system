package com.stockflow.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ReferenceDataForm {

    @Size(max = 30, message = "Code must not exceed 30 characters")
    @Pattern(regexp = "[A-Za-z0-9_-]*",
            message = "Code can contain only letters, numbers, hyphens and underscores")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 80, message = "Name must contain between 2 and 80 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private boolean active = true;

    public static ReferenceDataForm from(Category category) {
        ReferenceDataForm form = new ReferenceDataForm();
        form.setCode(category.getCode());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        return form;
    }

    public static ReferenceDataForm from(Brand brand) {
        ReferenceDataForm form = new ReferenceDataForm();
        form.setName(brand.getName());
        form.setActive(brand.isActive());
        return form;
    }

    public static ReferenceDataForm from(Unit unit) {
        ReferenceDataForm form = new ReferenceDataForm();
        form.setCode(unit.getCode());
        form.setName(unit.getName());
        form.setActive(unit.isActive());
        return form;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
