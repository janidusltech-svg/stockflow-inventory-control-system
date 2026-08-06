package com.stockflow.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuditTrailInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrailInterceptor.class);
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/(\\d+)(?:/|$)");

    private final AuditTrailService auditTrailService;

    public AuditTrailInterceptor(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (!shouldAudit(request)) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        String path = request.getRequestURI();
        String module = inferModule(path);
        String action = inferAction(path);
        boolean redirectedSuccessfully = exception == null
                && response.getStatus() >= 300 && response.getStatus() < 400;
        AuditOutcome outcome = redirectedSuccessfully ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE;
        String description = redirectedSuccessfully
                ? "Completed " + readable(action) + " in " + module + "."
                : "The " + readable(action) + " request in " + module
                + " did not complete a confirmed data change.";

        try {
            auditTrailService.record(
                    authentication.getName(), roleName(authentication), module, action, outcome,
                    request.getMethod(), path, reference(path), description,
                    response.getStatus(), request.getRemoteAddr());
        } catch (RuntimeException auditFailure) {
            LOGGER.warn("Audit event could not be stored for {} {}", request.getMethod(), path, auditFailure);
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (!(method.equals("POST") || method.equals("PUT")
                || method.equals("PATCH") || method.equals("DELETE"))) {
            return false;
        }
        String path = request.getRequestURI();
        return !path.equals("/login")
                && !path.equals("/logout")
                && !path.startsWith("/notifications")
                && !path.startsWith("/audit-trail");
    }

    private String roleName(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");
    }

    private String inferModule(String path) {
        if (startsWithAny(path, "/products", "/reference-data")) return "Product Catalogue";
        if (startsWithAny(path, "/suppliers", "/purchase-orders")) return "Purchasing";
        if (path.startsWith("/goods-receipts")) return "Goods Receiving";
        if (startsWithAny(path, "/warehouses", "/storage-locations", "/stock-transfers")) {
            return "Warehouses & Transfers";
        }
        if (startsWithAny(path, "/recipients", "/stock-issues", "/stock-returns")) {
            return "Issues & Returns";
        }
        if (startsWithAny(path, "/inventory-control", "/reorder-rules",
                "/stock-counts", "/stock-adjustments")) return "Inventory Control";
        if (path.startsWith("/user-accounts")) return "User Management";
        if (path.startsWith("/profile")) return "User Profile";
        return "Shared System";
    }

    private String inferAction(String path) {
        if (path.endsWith("/reset-password")) return "RESET_PASSWORD";
        if (path.equals("/profile/password")) return "CHANGE_PASSWORD";
        if (path.equals("/profile")) return "UPDATE_PROFILE";
        if (path.endsWith("/deactivate")) return "DEACTIVATE";
        if (path.endsWith("/activate")) return "ACTIVATE";
        if (path.endsWith("/approve")) return "APPROVE";
        if (path.endsWith("/reject")) return "REJECT";
        if (path.endsWith("/submit")) return "SUBMIT";
        if (path.endsWith("/close")) return "CLOSE";
        if (path.endsWith("/confirm")) return "CONFIRM";
        if (path.endsWith("/cancel")) return "CANCEL";
        if (path.endsWith("/dispatch")) return "DISPATCH";
        if (path.endsWith("/receive")) return "RECEIVE";
        if (path.endsWith("/status")) return "STATUS_CHANGE";
        if (path.endsWith("/remove") || path.endsWith("/delete")) return "DELETE";
        if (path.contains("/items")) {
            return path.matches(".*/items/\\d+$") ? "UPDATE_ITEM" : "ADD_ITEM";
        }
        if (path.equals("/warehouses/locations")) return "CREATE_LOCATION";
        if (path.matches("/warehouses/locations/\\d+")) return "UPDATE_LOCATION";
        return NUMERIC_SEGMENT.matcher(path).find() ? "UPDATE" : "CREATE";
    }

    private String reference(String path) {
        Matcher matcher = NUMERIC_SEGMENT.matcher(path);
        return matcher.find() ? "Record #" + matcher.group(1) : "New record";
    }

    private String readable(String action) {
        return action.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private boolean startsWithAny(String path, String... prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
