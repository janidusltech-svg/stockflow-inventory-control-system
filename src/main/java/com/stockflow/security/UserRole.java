package com.stockflow.security;

public enum UserRole {
    OPERATIONS_MANAGER(
            "Operations Manager",
            "Controls inventory, approvals, audits, reorder rules and management reporting."),
    STORE_SUPERVISOR(
            "Store Supervisor",
            "Manages daily catalogue, receiving, warehouse, transfer, issue and return work."),
    FINANCE_EXECUTIVE(
            "Finance Executive",
            "Maintains suppliers and purchase orders and reviews stock and receipt information."),
    CUSTOMER_SERVICE_COORDINATOR(
            "Customer Service Coordinator",
            "Views products, stock availability, recipients, issues and return status."),
    IT_SYSTEMS_OFFICER(
            "IT and Systems Officer",
            "Maintains user accounts and role assignments without changing business stock records.");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
