package com.stockflow;

import com.stockflow.control.ReorderRule;
import com.stockflow.control.ReorderRuleRepository;
import com.stockflow.issuing.Recipient;
import com.stockflow.issuing.RecipientRepository;
import com.stockflow.issuing.RecipientType;
import com.stockflow.product.Brand;
import com.stockflow.product.BrandRepository;
import com.stockflow.product.Category;
import com.stockflow.product.CategoryRepository;
import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import com.stockflow.product.Unit;
import com.stockflow.product.UnitRepository;
import com.stockflow.purchasing.Supplier;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.Warehouse;
import com.stockflow.warehouse.WarehouseRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the list pages with real rows so the new Delete buttons are
 * actually drawn, then submits Delete through security and CSRF like a browser.
 * Each test rolls back, so it leaves no data behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SafeDeletePageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ReorderRuleRepository reorderRuleRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StorageLocationRepository storageLocationRepository;
    @Autowired private RecipientRepository recipientRepository;

    private MockHttpSession operations;

    @BeforeEach
    void signIn() throws Exception {
        operations = login("operations");
    }

    @Test
    void listPagesShowDeleteOnlyForUnusedRecords() throws Exception {
        Product unused = newProduct("PAGE-UNUSED");
        Product withRule = newProduct("PAGE-RULE");
        ReorderRule rule = new ReorderRule();
        rule.setProduct(withRule);
        rule.setMinimumLevel(1);
        rule.setMaximumLevel(10);
        rule.setReorderQuantity(5);
        rule = reorderRuleRepository.saveAndFlush(rule);
        Brand brand = new Brand();
        brand.setName("Page Test Brand");
        brand = brandRepository.saveAndFlush(brand);

        mockMvc.perform(get("/products").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/products/" + unused.getId() + "/delete")))
                .andExpect(content().string(not(containsString("/products/" + withRule.getId() + "/delete"))))
                .andExpect(content().string(containsString("data-confirm-message=\"Permanently delete product PAGE-UNUSED?")));

        mockMvc.perform(get("/reference-data").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/reference-data/brands/" + brand.getId() + "/delete")))
                .andExpect(content().string(not(containsString(
                        "/reference-data/categories/" + unused.getCategory().getId() + "/delete"))));

        mockMvc.perform(get("/reorder-rules").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/reorder-rules/" + rule.getId() + "/delete")));
    }

    @Test
    void supplierWarehouseAndRecipientPagesRenderDeleteButtons() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-PAGE");
        supplier.setName("Page Supplier");
        supplier.setPhone("0771234567");
        supplier.setEmail("page@example.lk");
        supplier = supplierRepository.saveAndFlush(supplier);
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-PAGE");
        warehouse.setName("Page Warehouse");
        warehouse = warehouseRepository.saveAndFlush(warehouse);
        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setCode("BIN-PAGE");
        location.setName("Page Bin");
        location = storageLocationRepository.saveAndFlush(location);
        Recipient recipient = new Recipient();
        recipient.setCode("REC-PAGE");
        recipient.setName("Page Recipient");
        recipient.setRecipientType(RecipientType.CUSTOMER);
        recipient = recipientRepository.saveAndFlush(recipient);

        mockMvc.perform(get("/suppliers").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/suppliers/" + supplier.getId() + "/delete")));

        mockMvc.perform(get("/warehouses").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/warehouses/locations/" + location.getId() + "/delete")))
                // The warehouse still has a location, so it must not offer Delete yet.
                .andExpect(content().string(not(containsString("/warehouses/" + warehouse.getId() + "/delete"))));

        mockMvc.perform(get("/recipients").session(operations))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/recipients/" + recipient.getId() + "/delete")));
    }

    @Test
    void deleteButtonRemovesTheRowThroughTheRealRequest() throws Exception {
        Product product = newProduct("PAGE-POST");
        CsrfToken token = csrfFrom("/products");

        mockMvc.perform(post("/products/" + product.getId() + "/delete")
                        .session(operations)
                        .param(token.getParameterName(), token.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attribute("successMessage", "Product deleted permanently"));

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    void blockedDeleteShowsAMessageAndKeepsTheRow() throws Exception {
        Product product = newProduct("PAGE-BLOCK");
        ReorderRule rule = new ReorderRule();
        rule.setProduct(product);
        rule.setMinimumLevel(1);
        rule.setMaximumLevel(10);
        rule.setReorderQuantity(5);
        reorderRuleRepository.saveAndFlush(rule);
        CsrfToken token = csrfFrom("/products");

        mockMvc.perform(post("/products/" + product.getId() + "/delete")
                        .session(operations)
                        .param(token.getParameterName(), token.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", containsString("reorder rules")));

        assertThat(productRepository.findById(product.getId())).isPresent();
    }

    @Test
    void financeCannotDeleteProducts() throws Exception {
        Product product = newProduct("PAGE-ROLE");
        MockHttpSession finance = login("finance");
        MvcResult page = mockMvc.perform(get("/purchase-orders").session(finance)).andReturn();
        CsrfToken token = token(page);

        mockMvc.perform(post("/products/" + product.getId() + "/delete")
                        .session(finance)
                        .param(token.getParameterName(), token.getToken()))
                .andExpect(status().isForbidden());

        assertThat(productRepository.findById(product.getId())).isPresent();
    }

    private Product newProduct(String sku) {
        Category category = new Category();
        category.setCode(sku + "-C");
        category.setName(sku + " category");
        category = categoryRepository.saveAndFlush(category);
        Unit unit = new Unit();
        unit.setCode(sku + "-U");
        unit.setName(sku + " unit");
        unit = unitRepository.saveAndFlush(unit);
        Product product = new Product();
        product.setSku(sku);
        product.setName(sku + " product");
        product.setCategory(category);
        product.setUnit(unit);
        return productRepository.saveAndFlush(product);
    }

    private CsrfToken csrfFrom(String page) throws Exception {
        return token(mockMvc.perform(get(page).session(operations)).andReturn());
    }

    private CsrfToken token(MvcResult result) {
        CsrfToken token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) result.getRequest().getAttribute("_csrf");
        }
        if (token == null) {
            throw new IllegalStateException("The page did not expose a CSRF token");
        }
        return token;
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginPage.getRequest().getSession(false);
        if (session == null) {
            session = new MockHttpSession();
        }
        CsrfToken csrfToken = token(loginPage);

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
