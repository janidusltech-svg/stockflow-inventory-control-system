package com.stockflow;

import com.stockflow.control.ReorderRule;
import com.stockflow.control.ReorderRuleRepository;
import com.stockflow.issuing.Recipient;
import com.stockflow.issuing.RecipientRepository;
import com.stockflow.issuing.RecipientService;
import com.stockflow.issuing.RecipientType;
import com.stockflow.product.Category;
import com.stockflow.product.CategoryRepository;
import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import com.stockflow.product.ProductService;
import com.stockflow.product.ReferenceDataService;
import com.stockflow.product.Unit;
import com.stockflow.product.UnitRepository;
import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderForm;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.PurchaseOrderService;
import com.stockflow.purchasing.Supplier;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.purchasing.SupplierService;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.Warehouse;
import com.stockflow.warehouse.WarehouseRepository;
import com.stockflow.warehouse.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the safe-delete rules against the real H2 test database, so the
 * usage queries and the delete statements are checked end to end.
 * Each test rolls back, so it leaves no data behind.
 */
@SpringBootTest
@Transactional
class SafeDeleteIntegrationTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ReferenceDataService referenceDataService;
    @Autowired private ReorderRuleRepository reorderRuleRepository;
    @Autowired private SupplierService supplierService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private WarehouseService warehouseService;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StorageLocationRepository storageLocationRepository;
    @Autowired private RecipientService recipientService;
    @Autowired private RecipientRepository recipientRepository;

    @Test
    void neverUsedProductIsDeletedButItsCategoryAndUnitStay() {
        Product product = newProduct("DEL-TEST-1");

        assertThat(productService.findLinkedProductIds()).doesNotContain(product.getId());
        productService.delete(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
        assertThat(categoryRepository.findById(product.getCategory().getId())).isPresent();
        assertThat(unitRepository.findById(product.getUnit().getId())).isPresent();
    }

    @Test
    void productWithReorderRuleIsKept() {
        Product product = newProduct("DEL-TEST-2");
        ReorderRule rule = new ReorderRule();
        rule.setProduct(product);
        rule.setMinimumLevel(5);
        rule.setMaximumLevel(50);
        rule.setReorderQuantity(20);
        reorderRuleRepository.saveAndFlush(rule);

        assertThat(productService.findLinkedProductIds()).contains(product.getId());
        assertThatThrownBy(() -> productService.delete(product.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reorder rules");
        assertThat(productRepository.findById(product.getId())).isPresent();
    }

    @Test
    void categoryInUseIsKeptAndUnusedCategoryIsDeleted() {
        Product product = newProduct("DEL-TEST-3");
        Long usedCategoryId = product.getCategory().getId();

        assertThatThrownBy(() -> referenceDataService.deleteCategory(usedCategoryId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 product(s) use it");

        Category unused = new Category();
        unused.setCode("DEL-UNUSED");
        unused.setName("Unused delete-test category");
        unused = categoryRepository.saveAndFlush(unused);
        referenceDataService.deleteCategory(unused.getId());

        assertThat(categoryRepository.findById(unused.getId())).isEmpty();
        assertThat(categoryRepository.findById(usedCategoryId)).isPresent();
    }

    @Test
    void draftPurchaseOrderAndItsItemsAreDeletedThenSupplierBecomesDeletable() {
        Supplier supplier = newSupplier("SUP-DEL-1");
        Product product = newProduct("DEL-TEST-4");

        PurchaseOrderForm form = new PurchaseOrderForm();
        form.setSupplierId(supplier.getId());
        form.setOrderDate(LocalDate.now());
        PurchaseOrder order = purchaseOrderService.create(form);
        com.stockflow.purchasing.PurchaseOrderItemForm itemForm =
                new com.stockflow.purchasing.PurchaseOrderItemForm();
        itemForm.setProductId(product.getId());
        itemForm.setQuantity(10);
        itemForm.setUnitPrice(new java.math.BigDecimal("150.00"));
        purchaseOrderService.addItem(order.getId(), itemForm);

        // While the draft exists, the supplier and the product are both protected.
        assertThatThrownBy(() -> supplierService.delete(supplier.getId()))
                .hasMessageContaining("1 purchase order(s)");
        assertThat(productService.findDeleteBlockers(product.getId())).contains("purchase orders");

        purchaseOrderService.deleteDraft(order.getId());
        assertThat(purchaseOrderRepository.findById(order.getId())).isEmpty();

        supplierService.delete(supplier.getId());
        assertThat(supplierRepository.findById(supplier.getId())).isEmpty();
    }

    @Test
    void approvedPurchaseOrderIsKept() {
        Supplier supplier = newSupplier("SUP-DEL-2");
        Product product = newProduct("DEL-TEST-5");
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.setSupplierId(supplier.getId());
        form.setOrderDate(LocalDate.now());
        PurchaseOrder order = purchaseOrderService.create(form);
        com.stockflow.purchasing.PurchaseOrderItemForm itemForm =
                new com.stockflow.purchasing.PurchaseOrderItemForm();
        itemForm.setProductId(product.getId());
        itemForm.setQuantity(4);
        itemForm.setUnitPrice(new java.math.BigDecimal("80.00"));
        purchaseOrderService.addItem(order.getId(), itemForm);
        purchaseOrderService.changeStatus(order.getId(),
                com.stockflow.purchasing.PurchaseOrderStatus.APPROVED);

        assertThatThrownBy(() -> purchaseOrderService.deleteDraft(order.getId()))
                .hasMessageContaining("Only draft purchase orders can be deleted");
        assertThat(purchaseOrderRepository.findById(order.getId())).isPresent();
    }

    @Test
    void warehouseIsDeletableOnlyAfterItsUnusedLocationIsDeleted() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-DEL");
        warehouse.setName("Delete Test Warehouse");
        warehouse = warehouseRepository.saveAndFlush(warehouse);
        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setCode("BIN-DEL");
        location.setName("Delete Test Bin");
        location = storageLocationRepository.saveAndFlush(location);
        Long warehouseId = warehouse.getId();
        Long locationId = location.getId();

        assertThatThrownBy(() -> warehouseService.deleteWarehouse(warehouseId))
                .hasMessageContaining("1 storage location(s)");
        assertThat(warehouseService.findLinkedLocationIds()).doesNotContain(locationId);

        warehouseService.deleteLocation(locationId);
        warehouseService.deleteWarehouse(warehouseId);

        assertThat(storageLocationRepository.findById(locationId)).isEmpty();
        assertThat(warehouseRepository.findById(warehouseId)).isEmpty();
    }

    @Test
    void recipientWithoutIssuesIsDeleted() {
        Recipient recipient = new Recipient();
        recipient.setCode("REC-DEL");
        recipient.setName("Delete Test Recipient");
        recipient.setRecipientType(RecipientType.CUSTOMER);
        recipient = recipientRepository.saveAndFlush(recipient);

        assertThat(recipientService.findRecipientIdsWithStockIssues()).doesNotContain(recipient.getId());
        recipientService.delete(recipient.getId());

        assertThat(recipientRepository.findById(recipient.getId())).isEmpty();
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

    private Supplier newSupplier(String code) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(code);
        supplier.setName(code + " supplier");
        supplier.setPhone("0771234567");
        supplier.setEmail(code.toLowerCase() + "@example.lk");
        return supplierService.create(supplier);
    }
}
