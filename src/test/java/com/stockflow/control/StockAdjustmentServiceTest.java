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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

class StockAdjustmentServiceTest {

    private StockAdjustmentRepository repository;
    private InventoryBalanceRepository balanceRepository;
    private InventoryService inventoryService;
    private StockAdjustmentService service;

    @BeforeEach
    void setUp() {
        repository = mock(StockAdjustmentRepository.class);
        inventoryService = mock(InventoryService.class);
        balanceRepository = mock(InventoryBalanceRepository.class);
        service = new StockAdjustmentService(
                repository,
                balanceRepository,
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

    @Test
    void draftAdjustmentIsDeletedWithoutChangingStock() {
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setAdjustmentNumber("ADJ-20261002-0002");
        when(repository.findByIdWithDetails(2L)).thenReturn(Optional.of(adjustment));

        service.deleteDraft(2L);

        verify(repository).delete(adjustment);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void appliedAdjustmentCannotBeDeleted() {
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setStatus(AdjustmentStatus.APPLIED);
        when(repository.findByIdWithDetails(3L)).thenReturn(Optional.of(adjustment));

        assertThatThrownBy(() -> service.deleteDraft(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only draft adjustments can be changed");

        verify(repository, never()).delete(any(StockAdjustment.class));
    }

    @Test
    void newAdjustmentSkipsANumberThatStillExistsAfterADraftWasDeleted() {
        InventoryBalance balance = new InventoryBalance();
        balance.setQuantityAvailable(20);
        when(balanceRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(balance));
        when(repository.save(any(StockAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // ADJ ...-0001 and ...-0003 remain after draft ...-0002 was deleted, so count() is 2.
        when(repository.count()).thenReturn(2L);
        when(repository.existsByAdjustmentNumber(any(String.class)))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).endsWith("-0003"));

        StockAdjustmentForm form = new StockAdjustmentForm();
        form.setInventoryBalanceId(1L);
        form.setQuantityChange(5);
        form.setReason("Found extra stock");

        StockAdjustment created = service.create(form);

        assertThat(created.getAdjustmentNumber()).endsWith("-0004");
    }
}
