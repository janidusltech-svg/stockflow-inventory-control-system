package com.stockflow.receiving;

import com.stockflow.inventory.BatchService;
import com.stockflow.inventory.InventoryService;
import com.stockflow.product.Product;
import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderItem;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.PurchaseOrderStatus;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsReceiptServiceTest {

    private GoodsReceiptRepository goodsReceiptRepository;
    private PurchaseOrderRepository purchaseOrderRepository;
    private StorageLocationRepository storageLocationRepository;
    private BatchService batchService;
    private InventoryService inventoryService;
    private GoodsReceiptService goodsReceiptService;

    @BeforeEach
    void setUp() {
        goodsReceiptRepository = mock(GoodsReceiptRepository.class);
        purchaseOrderRepository = mock(PurchaseOrderRepository.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        batchService = mock(BatchService.class);
        inventoryService = mock(InventoryService.class);
        goodsReceiptService = new GoodsReceiptService(
                goodsReceiptRepository,
                purchaseOrderRepository,
                storageLocationRepository,
                batchService,
                inventoryService
        );
    }

    @Test
    void createsDraftForApprovedPurchaseOrder() {
        PurchaseOrder order = mock(PurchaseOrder.class);
        PurchaseOrderItem orderItem = mock(PurchaseOrderItem.class);
        when(order.getId()).thenReturn(1L);
        when(order.getStatus()).thenReturn(PurchaseOrderStatus.APPROVED);
        when(order.getOrderDate()).thenReturn(LocalDate.of(2026, 7, 20));
        when(order.getItems()).thenReturn(List.of(orderItem));
        when(orderItem.getRemainingQuantity()).thenReturn(10);

        Warehouse warehouse = mock(Warehouse.class);
        StorageLocation location = mock(StorageLocation.class);
        when(location.isActive()).thenReturn(true);
        when(location.getWarehouse()).thenReturn(warehouse);
        when(warehouse.isActive()).thenReturn(true);

        when(purchaseOrderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(storageLocationRepository.findByIdWithWarehouse(2L)).thenReturn(Optional.of(location));
        when(goodsReceiptRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(goodsReceiptRepository.save(any(GoodsReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoodsReceiptForm form = form(1L, 2L);
        GoodsReceipt created = goodsReceiptService.create(form);

        assertThat(created.getReceiptNumber()).startsWith("GRN-");
        assertThat(created.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
        assertThat(created.getPurchaseOrder()).isEqualTo(order);
        verify(goodsReceiptRepository).save(created);
    }

    @Test
    void rejectsPurchaseOrderThatIsNotApproved() {
        PurchaseOrder order = mock(PurchaseOrder.class);
        when(order.getStatus()).thenReturn(PurchaseOrderStatus.DRAFT);
        when(order.getItems()).thenReturn(List.of());
        when(purchaseOrderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> goodsReceiptService.create(form(1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved or partially received");
    }

    @Test
    void confirmationUpdatesInventoryAndCompletesPurchaseOrder() {
        Product product = mock(Product.class);
        StorageLocation location = mock(StorageLocation.class);
        PurchaseOrder order = mock(PurchaseOrder.class);
        PurchaseOrderItem orderItem = mock(PurchaseOrderItem.class);

        AtomicInteger receivedQuantity = new AtomicInteger(0);
        when(order.getId()).thenReturn(1L);
        when(order.getStatus()).thenReturn(PurchaseOrderStatus.APPROVED);
        when(order.getItems()).thenReturn(List.of(orderItem));
        when(orderItem.getId()).thenReturn(5L);
        when(orderItem.getProduct()).thenReturn(product);
        when(orderItem.getReceivedQuantity()).thenAnswer(invocation -> receivedQuantity.get());
        when(orderItem.getRemainingQuantity()).thenAnswer(invocation -> 10 - receivedQuantity.get());
        doAnswer(invocation -> {
            receivedQuantity.set(invocation.getArgument(0));
            return null;
        }).when(orderItem).setReceivedQuantity(any(Integer.class));

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNumber("GRN-2026-0001");
        receipt.setPurchaseOrder(order);
        receipt.setLocation(location);
        receipt.setReceivedDate(LocalDate.of(2026, 7, 30));

        GoodsReceiptItem receiptItem = new GoodsReceiptItem();
        receiptItem.setGoodsReceipt(receipt);
        receiptItem.setPurchaseOrderItem(orderItem);
        receiptItem.setProduct(product);
        receiptItem.setDeliveredQuantity(10);
        receiptItem.setAcceptedQuantity(10);
        receiptItem.setRejectedQuantity(0);
        receipt.getItems().add(receiptItem);

        when(goodsReceiptRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(receipt));
        when(purchaseOrderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        goodsReceiptService.confirm(10L);

        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
        assertThat(receipt.getConfirmedAt()).isNotNull();
        assertThat(receivedQuantity.get()).isEqualTo(10);
        verify(inventoryService).receive(product, location, null, 10, "GRN-2026-0001");
        verify(order).setStatus(PurchaseOrderStatus.RECEIVED);
        verify(goodsReceiptRepository).save(receipt);
    }

    private GoodsReceiptForm form(Long purchaseOrderId, Long locationId) {
        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseOrderId(purchaseOrderId);
        form.setLocationId(locationId);
        form.setReceivedDate(LocalDate.of(2026, 7, 30));
        form.setSupplierDeliveryReference("DN-4501");
        return form;
    }
}
