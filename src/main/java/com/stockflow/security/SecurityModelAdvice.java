package com.stockflow.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;

@ControllerAdvice
public class SecurityModelAdvice {

    @ModelAttribute("authenticatedUser")
    public boolean authenticatedUser(Authentication authentication) {
        return currentRole(authentication) != null;
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        return currentRole(authentication) == null ? "" : authentication.getName();
    }

    @ModelAttribute("currentRoleLabel")
    public String currentRoleLabel(Authentication authentication) {
        UserRole role = currentRole(authentication);
        return role == null ? "" : role.getDisplayName();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }

    @ModelAttribute("currentPageReadOnly")
    public boolean currentPageReadOnly(Authentication authentication,
                                       HttpServletRequest request) {
        UserRole role = currentRole(authentication);
        String path = request == null ? "" : request.getRequestURI();
        if (role == null || path.equals("/dashboard")) {
            return false;
        }
        if (role == UserRole.CUSTOMER_SERVICE_COORDINATOR) {
            return startsWithAny(path, "/products", "/inventory-control",
                    "/recipients", "/stock-issues", "/stock-returns");
        }
        if (role == UserRole.FINANCE_EXECUTIVE) {
            return startsWithAny(path, "/products", "/goods-receipts", "/inventory-control");
        }
        if (role == UserRole.STORE_SUPERVISOR) {
            return startsWithAny(path, "/suppliers", "/purchase-orders");
        }
        return false;
    }

    @ModelAttribute("canViewProducts")
    public boolean canViewProducts(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR,
                UserRole.FINANCE_EXECUTIVE,
                UserRole.CUSTOMER_SERVICE_COORDINATOR);
    }

    @ModelAttribute("canManageProducts")
    public boolean canManageProducts(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR);
    }

    @ModelAttribute("canViewPurchasing")
    public boolean canViewPurchasing(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR,
                UserRole.FINANCE_EXECUTIVE);
    }

    @ModelAttribute("canManagePurchasing")
    public boolean canManagePurchasing(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.FINANCE_EXECUTIVE);
    }

    @ModelAttribute("canViewReceiving")
    public boolean canViewReceiving(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR,
                UserRole.FINANCE_EXECUTIVE);
    }

    @ModelAttribute("canManageReceiving")
    public boolean canManageReceiving(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR);
    }

    @ModelAttribute("canManageWarehouses")
    public boolean canManageWarehouses(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR);
    }

    @ModelAttribute("canViewIssuesAndReturns")
    public boolean canViewIssuesAndReturns(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR,
                UserRole.CUSTOMER_SERVICE_COORDINATOR);
    }

    @ModelAttribute("canManageIssuesAndReturns")
    public boolean canManageIssuesAndReturns(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR);
    }

    @ModelAttribute("canManageInventoryControl")
    public boolean canManageInventoryControl(Authentication authentication) {
        return hasAnyRole(authentication, UserRole.OPERATIONS_MANAGER);
    }

    @ModelAttribute("canViewStockOnly")
    public boolean canViewStockOnly(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.STORE_SUPERVISOR,
                UserRole.FINANCE_EXECUTIVE,
                UserRole.CUSTOMER_SERVICE_COORDINATOR);
    }

    @ModelAttribute("canManageUsers")
    public boolean canManageUsers(Authentication authentication) {
        return hasAnyRole(authentication, UserRole.IT_SYSTEMS_OFFICER);
    }

    @ModelAttribute("canViewReports")
    public boolean canViewReports(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.STORE_SUPERVISOR,
                UserRole.FINANCE_EXECUTIVE);
    }

    @ModelAttribute("canViewAuditTrail")
    public boolean canViewAuditTrail(Authentication authentication) {
        return hasAnyRole(authentication,
                UserRole.OPERATIONS_MANAGER,
                UserRole.IT_SYSTEMS_OFFICER);
    }

    private boolean hasAnyRole(Authentication authentication, UserRole... allowedRoles) {
        UserRole currentRole = currentRole(authentication);
        return currentRole != null && Arrays.asList(allowedRoles).contains(currentRole);
    }

    private UserRole currentRole(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .map(this::safeRole)
                .filter(role -> role != null)
                .findFirst()
                .orElse(null);
    }

    private UserRole safeRole(String roleName) {
        try {
            return UserRole.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean startsWithAny(String path, String... prefixes) {
        return Arrays.stream(prefixes).anyMatch(path::startsWith);
    }
}
