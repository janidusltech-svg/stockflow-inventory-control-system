package com.stockflow;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RouteSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession operationsSession;

    @BeforeEach
    void signInAsOperationsManager() throws Exception {
        operationsSession = login("operations");
    }

    @ParameterizedTest(name = "GET {0} renders successfully")
    @ValueSource(strings = {
            "/dashboard",
            "/products",
            "/reference-data",
            "/suppliers",
            "/purchase-orders",
            "/goods-receipts",
            "/warehouses",
            "/stock-transfers",
            "/recipients",
            "/stock-issues",
            "/stock-returns",
            "/inventory-control",
            "/inventory-control/stock",
            "/inventory-control/movements",
            "/reorder-rules",
            "/stock-counts",
            "/stock-adjustments"
    })
    void mainPagesRenderWithoutServerErrors(String route) throws Exception {
        mockMvc.perform(get(route).session(operationsSession))
                .andExpect(status().isOk());
    }

    @Test
    void landingPageContainsWelcomeAndWarehouseExperiences() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"site-loader\"")))
                .andExpect(content().string(containsString("Welcome to StockFlow")))
                .andExpect(content().string(containsString("class=\"hero-sticky\"")))
                .andExpect(content().string(containsString("id=\"hero-worker-cart\"")))
                .andExpect(content().string(containsString("id=\"hero-progress-path\"")))
                .andExpect(content().string(containsString("Product movement preview")));
    }

    @Test
    void anonymousUsersAreSentToTheLoginPage() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void customerServiceCanViewIssuesButCannotOpenPurchasing() throws Exception {
        MockHttpSession session = login("service");

        mockMvc.perform(get("/stock-issues").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/purchase-orders").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeCanViewPurchasingButCannotOpenWarehouseManagement() throws Exception {
        MockHttpSession session = login("finance");

        mockMvc.perform(get("/purchase-orders").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/warehouses").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reference-data").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void storeSupervisorCanManageProductReferenceData() throws Exception {
        MockHttpSession session = login("store");

        mockMvc.perform(get("/reference-data").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Product Categories")))
                .andExpect(content().string(containsString("Measurement Units")));
    }

    @Test
    void itOfficerCanManageUsersButCannotOpenProductOperations() throws Exception {
        MockHttpSession session = login("itadmin");

        mockMvc.perform(get("/user-accounts").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/products").session(session))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession(false);
        if (session == null) {
            session = new MockHttpSession();
        }

        CsrfToken csrfToken = (CsrfToken) loginPage.getRequest()
                .getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) loginPage.getRequest().getAttribute("_csrf");
        }
        if (csrfToken == null) {
            throw new IllegalStateException("The login page did not expose a CSRF token");
        }

        MvcResult result = mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", username)
                        .param("password", "StockFlow@2026")
                        .param(csrfToken.getParameterName(), csrfToken.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        HttpSession authenticatedSession = result.getRequest().getSession(false);
        if (authenticatedSession instanceof MockHttpSession mockSession) {
            return mockSession;
        }
        return session;
    }
}
