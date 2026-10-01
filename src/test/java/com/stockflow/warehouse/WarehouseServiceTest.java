package com.stockflow.warehouse;

import com.stockflow.inventory.InventoryBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class WarehouseServiceTest {

    private WarehouseRepository warehouseRepository;
    private StorageLocationRepository storageLocationRepository;
    private InventoryBalanceRepository inventoryBalanceRepository;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseRepository = mock(WarehouseRepository.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        inventoryBalanceRepository = mock(InventoryBalanceRepository.class);
        warehouseService = new WarehouseService(
                warehouseRepository,
                storageLocationRepository,
                inventoryBalanceRepository
        );
    }

    @Test
    void createsWarehouseWithNormalizedCode() {
        WarehouseForm form = new WarehouseForm();
        form.setCode("wh-north");
        form.setName("Northern Warehouse");
        form.setAddress("Kandy Road");
        form.setActive(true);

        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse saved = warehouseService.createWarehouse(form);

        assertThat(saved.getCode()).isEqualTo("WH-NORTH");
        assertThat(saved.getName()).isEqualTo("Northern Warehouse");
        assertThat(saved.isActive()).isTrue();
        verify(warehouseRepository).save(saved);
    }

    @Test
    void preventsDeactivationWhenWarehouseStillHasStock() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-MAIN");
        warehouse.setName("Main Warehouse");
        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));
        when(inventoryBalanceRepository.countPositiveByWarehouseId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Move all stock out");
    }

    @Test
    void warehouseWithoutLocationsIsDeletedPermanently() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-EMPTY");
        when(warehouseRepository.findById(4L)).thenReturn(java.util.Optional.of(warehouse));
        when(storageLocationRepository.countByWarehouseId(4L)).thenReturn(0L);

        warehouseService.deleteWarehouse(4L);

        verify(warehouseRepository).delete(warehouse);
    }

    @Test
    void warehouseWithLocationsIsKept() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-MAIN");
        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));
        when(storageLocationRepository.countByWarehouseId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> warehouseService.deleteWarehouse(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 storage location(s)");

        verify(warehouseRepository, never()).delete(any(Warehouse.class));
    }

    @Test
    void unusedLocationIsDeletedPermanently() {
        StorageLocation location = location("NEW-BIN");
        when(storageLocationRepository.findByIdWithWarehouse(8L)).thenReturn(java.util.Optional.of(location));

        warehouseService.deleteLocation(8L);

        verify(storageLocationRepository).delete(location);
    }

    @Test
    void locationWithStockHistoryIsKept() {
        StorageLocation location = location("RECEIVING");
        when(storageLocationRepository.findByIdWithWarehouse(1L)).thenReturn(java.util.Optional.of(location));
        when(storageLocationRepository.findLocationIdsUsedInStockMovements()).thenReturn(java.util.List.of(1L));
        when(storageLocationRepository.findLocationIdsUsedAsTransferDestination()).thenReturn(java.util.List.of(1L));

        assertThatThrownBy(() -> warehouseService.deleteLocation(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock movements")
                .hasMessageContaining("stock transfers (destination)")
                .hasMessageContaining("Deactivate it instead");

        verify(storageLocationRepository, never()).delete(any(StorageLocation.class));
    }

    private StorageLocation location(String code) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-MAIN");
        warehouse.setName("Main Warehouse");
        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setCode(code);
        location.setName("Test location");
        return location;
    }
}
