package com.stockflow;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvaluationReadinessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allSixMajorFunctionsAndSharedManagementPagesOpenForOperations() throws Exception {
        MockHttpSession session = login("operations");
        List<String> routes = List.of(
                "/products",
                "/purchase-orders",
                "/goods-receipts",
                "/stock-transfers",
                "/stock-issues",
                "/inventory-control",
                "/reports",
                "/audit-trail",
                "/notifications",
                "/search?q=demo",
                "/profile"
        );

        for (String route : routes) {
            mockMvc.perform(get(route).session(session))
                    .andExpect(status().isOk());
        }
    }

    @ParameterizedTest(name = "{0}: allowed {1}, denied {2}")
    @MethodSource("roleAccessExamples")
    void roleAccessMatrixRemainsEnforced(String username, String allowedRoute,
                                         String forbiddenRoute) throws Exception {
        MockHttpSession session = login(username);

        mockMvc.perform(get(allowedRoute).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get(forbiddenRoute).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiredTablesExistAndConfirmedInventoryCannotStartNegative() {
        List<String> requiredTables = List.of(
                "categories", "brands", "units", "products",
                "suppliers", "purchase_orders", "purchase_order_items",
                "warehouses", "storage_locations", "batches",
                "goods_receipts", "goods_receipt_items",
                "inventory_balances", "stock_movements",
                "stock_transfers", "stock_transfer_items",
                "recipients", "stock_issues", "stock_issue_items",
                "stock_returns", "stock_return_items",
                "reorder_rules", "stock_counts", "stock_count_items",
                "stock_adjustments", "system_users", "system_notifications", "audit_events"
        );

        for (String table : requiredTables) {
            Long count = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
            assertThat(count).as("table %s should be queryable", table).isNotNull().isGreaterThanOrEqualTo(0L);
        }

        Long negativeBalances = jdbcTemplate.queryForObject(
                "select count(*) from inventory_balances where quantity_available < 0", Long.class);
        assertThat(negativeBalances).isZero();

        Long seededUsers = jdbcTemplate.queryForObject(
                "select count(*) from system_users where active = true", Long.class);
        assertThat(seededUsers).isGreaterThanOrEqualTo(5L);
    }

    private static Stream<Arguments> roleAccessExamples() {
        return Stream.of(
                Arguments.of("store", "/stock-transfers", "/audit-trail"),
                Arguments.of("finance", "/reports", "/warehouses"),
                Arguments.of("service", "/stock-issues", "/reports"),
                Arguments.of("itadmin", "/audit-trail", "/products")
        );
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession(false);
        if (session == null) {
            session = new MockHttpSession();
        }
        CsrfToken csrfToken = csrfToken(loginPage);

        MvcResult result = mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", username)
                        .param("password", "StockFlow@2026")
                        .param(csrfToken.getParameterName(), csrfToken.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        HttpSession authenticatedSession = result.getRequest().getSession(false);
        return authenticatedSession instanceof MockHttpSession mockSession ? mockSession : session;
    }

    private CsrfToken csrfToken(MvcResult page) {
        CsrfToken csrfToken = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) page.getRequest().getAttribute("_csrf");
        }
        if (csrfToken == null) {
            throw new IllegalStateException("The login page did not expose a CSRF token");
        }
        return csrfToken;
    }
}
