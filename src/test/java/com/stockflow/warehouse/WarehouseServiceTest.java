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
}
