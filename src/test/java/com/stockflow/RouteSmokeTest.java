package com.stockflow;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RouteSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "GET {0} renders successfully")
    @ValueSource(strings = {
            "/",
            "/products",
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
        mockMvc.perform(get(route))
                .andExpect(status().isOk());
    }
}
