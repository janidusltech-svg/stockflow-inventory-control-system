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

class StockTransferServiceTest {

    private StockTransferService stockTransferService;
    private StorageLocationRepository storageLocationRepository;

    @BeforeEach
    void setUp() {
        StockTransferRepository stockTransferRepository = mock(StockTransferRepository.class);
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
