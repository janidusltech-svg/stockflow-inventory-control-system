package com.stockflow.issuing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RecipientForm {

    @NotBlank(message = "Recipient code is required")
    @Size(max = 30, message = "Recipient code must be 30 characters or less")
    @Pattern(regexp = "[A-Za-z0-9-]+", message = "Use only letters, numbers and hyphens in the code")
    private String code;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 120, message = "Recipient name must be 120 characters or less")
    private String name;

    @NotNull(message = "Recipient type is required")
    private RecipientType recipientType;

    @Size(max = 100, message = "Contact name must be 100 characters or less")
    private String contactName;

    @Size(max = 30, message = "Phone number must be 30 characters or less")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email must be 120 characters or less")
    private String email;

    @Size(max = 255, message = "Address must be 255 characters or less")
    private String address;

    private boolean active = true;

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

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
