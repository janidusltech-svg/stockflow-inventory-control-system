package com.stockflow.purchasing;

import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseOrderServiceTest {

    private PurchaseOrderRepository orderRepository;
    private SupplierService supplierService;
    private ProductRepository productRepository;
    private PurchaseOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(PurchaseOrderRepository.class);
        supplierService = mock(SupplierService.class);
        productRepository = mock(ProductRepository.class);
        orderService = new PurchaseOrderService(orderRepository, supplierService, productRepository);
    }

    @Test
    void createsDraftOrderWithGeneratedOrderNumber() {
        Supplier supplier = activeSupplier(1L);
        PurchaseOrderForm form = orderForm(1L);

        when(supplierService.findById(1L)).thenReturn(supplier);
        when(orderRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(orderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder created = orderService.create(form);

        assertThat(created.getOrderNumber()).startsWith("PO-");
        assertThat(created.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(created.getSupplier()).isEqualTo(supplier);
        verify(orderRepository).save(created);
    }

    @Test
    void rejectsExpectedDateBeforeOrderDate() {
        PurchaseOrderForm form = orderForm(1L);
        form.setExpectedDate(form.getOrderDate().minusDays(1));

        assertThatThrownBy(() -> orderService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected date cannot be before the order date");
    }

    @Test
    void addsActiveProductToDraftOrder() {
        PurchaseOrder order = new PurchaseOrder();
        Product product = activeProduct(2L);
        PurchaseOrderItemForm itemForm = new PurchaseOrderItemForm();
        itemForm.setProductId(2L);
        itemForm.setQuantity(4);
        itemForm.setUnitPrice(new BigDecimal("750.00"));

        when(orderRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(order));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        orderService.addItem(10L, itemForm);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("3000.00");
        verify(orderRepository).save(order);
    }

    @Test
    void refusesToApproveOrderWithoutItems() {
        PurchaseOrder order = new PurchaseOrder();
        when(orderRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(10L, PurchaseOrderStatus.APPROVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Add at least one item before approving the purchase order");
    }

    @Test
    void draftOrderIsDeletedTogetherWithItsOwnItemLines() {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber("PO-2026-0009");
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(order);
        item.setProduct(activeProduct(1L));
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("100.00"));
        order.getItems().add(item);
        when(orderRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(order));
        when(orderRepository.countGoodsReceiptsByPurchaseOrderId(9L)).thenReturn(0L);

        orderService.deleteDraft(9L);

        verify(orderRepository).delete(order);
    }

    @Test
    void approvedOrderCannotBeDeleted() {
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(PurchaseOrderStatus.APPROVED);
        when(orderRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteDraft(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only draft purchase orders can be deleted");

        verify(orderRepository, never()).delete(any(PurchaseOrder.class));
    }

    @Test
    void draftOrderLinkedToGoodsReceiptIsKept() {
        PurchaseOrder order = new PurchaseOrder();
        when(orderRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(order));
        when(orderRepository.countGoodsReceiptsByPurchaseOrderId(4L)).thenReturn(1L);

        assertThatThrownBy(() -> orderService.deleteDraft(4L))
                .hasMessageContaining("has goods receipts");

        verify(orderRepository, never()).delete(any(PurchaseOrder.class));
    }

    private PurchaseOrderForm orderForm(Long supplierId) {
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.setSupplierId(supplierId);
        form.setOrderDate(LocalDate.of(2026, 7, 25));
        form.setExpectedDate(LocalDate.of(2026, 7, 30));
        form.setNotes("Deliver during business hours");
        return form;
    }

    private Supplier activeSupplier(Long id) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierCode("SUP-001");
        supplier.setName("Fresh Foods Lanka");
        supplier.setActive(true);
        return supplier;
    }

    private Product activeProduct(Long id) {
        Product product = new Product();
        product.setSku("RICE-5KG");
        product.setName("Rice 5kg");
        product.setActive(true);
        return product;
    }
}
