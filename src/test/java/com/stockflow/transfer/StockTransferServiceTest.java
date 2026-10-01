package com.stockflow.transfer;

import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

class StockTransferServiceTest {

    private StockTransferService stockTransferService;
    private StockTransferRepository stockTransferRepository;
    private StorageLocationRepository storageLocationRepository;

    @BeforeEach
    void setUp() {
        stockTransferRepository = mock(StockTransferRepository.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        InventoryBalanceRepository inventoryBalanceRepository =
                mock(InventoryBalanceRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);
        stockTransferService = new StockTransferService(
                stockTransferRepository,
                storageLocationRepository,
                inventoryBalanceRepository,
                inventoryService
        );
    }

    @Test
    void rejectsSameSourceAndDestination() {
        StorageLocation location = activeLocation();
        when(storageLocationRepository.findByIdWithWarehouse(1L))
                .thenReturn(Optional.of(location));

        StockTransferForm form = new StockTransferForm();
        form.setSourceLocationId(1L);
        form.setDestinationLocationId(1L);
        form.setTransferDate(LocalDate.now());

        assertThatThrownBy(() -> stockTransferService.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    void draftTransferIsDeletedTogetherWithItsOwnItemLines() {
        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber("TRF-2026-0003");
        when(stockTransferRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(transfer));

        stockTransferService.deleteDraft(3L);

        verify(stockTransferRepository).delete(transfer);
    }

    @Test
    void dispatchedTransferCannotBeDeleted() {
        StockTransfer transfer = new StockTransfer();
        transfer.setStatus(StockTransferStatus.DISPATCHED);
        when(stockTransferRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.deleteDraft(4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only draft transfers can be changed");

        verify(stockTransferRepository, never()).delete(any(StockTransfer.class));
    }

    private StorageLocation activeLocation() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-MAIN");
        warehouse.setName("Main Warehouse");
        warehouse.setActive(true);

        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setCode("RECEIVING");
        location.setName("Receiving Area");
        location.setActive(true);

        try {
            java.lang.reflect.Field id = StorageLocation.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(location, 1L);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return location;
    }
}
