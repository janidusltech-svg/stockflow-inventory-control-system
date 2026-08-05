package com.stockflow.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserAccountForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must not exceed 120 characters")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
            message = "Use 3-50 letters, numbers, dots, underscores or hyphens")
    private String username;

    @NotBlank(message = "Email address is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @NotNull(message = "Select a user role")
    private UserRole role;

    private String password;

    private boolean active = true;

    public static UserAccountForm from(UserAccount account) {
        UserAccountForm form = new UserAccountForm();
        form.setFullName(account.getFullName());
        form.setUsername(account.getUsername());
        form.setEmail(account.getEmail());
        form.setRole(account.getRole());
        form.setActive(account.isActive());
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
