package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockAdjustmentServiceTest {

    private StockAdjustmentRepository repository;
    private InventoryService inventoryService;
    private StockAdjustmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(StockAdjustmentRepository.class);
        inventoryService = mock(InventoryService.class);
        service = new StockAdjustmentService(
                repository,
                mock(InventoryBalanceRepository.class),
                inventoryService
        );
    }

    @Test
    void approvalAppliesQuantityAndCompletesAuditRecord() {
        Product product = mock(Product.class);
        StorageLocation location = mock(StorageLocation.class);
        InventoryBalance balance = new InventoryBalance();
        balance.setProduct(product);
        balance.setLocation(location);
        balance.setQuantityAvailable(12);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setAdjustmentNumber("ADJ-20260805-0001");
        adjustment.setInventoryBalance(balance);
        adjustment.setQuantityChange(-2);
        adjustment.setReason("Physical count variance");
        adjustment.setStatus(AdjustmentStatus.PENDING);
        when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(adjustment));

        service.approve(1L);

        verify(inventoryService).adjust(product, location, null, -2, "ADJ-20260805-0001");
        verify(repository).save(adjustment);
        assertThat(adjustment.getStatus()).isEqualTo(AdjustmentStatus.APPLIED);
        assertThat(adjustment.getReviewedAt()).isNotNull();
    }
}
