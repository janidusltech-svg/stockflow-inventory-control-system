package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockCountServiceTest {

    private StockCountRepository stockCountRepository;
    private StorageLocationRepository locationRepository;
    private InventoryBalanceRepository balanceRepository;
    private StockAdjustmentRepository adjustmentRepository;
    private StockCountService service;

    @BeforeEach
    void setUp() {
        stockCountRepository = mock(StockCountRepository.class);
        locationRepository = mock(StorageLocationRepository.class);
        balanceRepository = mock(InventoryBalanceRepository.class);
        adjustmentRepository = mock(StockAdjustmentRepository.class);
        service = new StockCountService(
                stockCountRepository, locationRepository, balanceRepository, adjustmentRepository
        );
    }

    @Test
    void createSessionSnapshotsCurrentSystemQuantity() {
        Warehouse warehouse = new Warehouse();
        warehouse.setActive(true);
        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setActive(true);
        InventoryBalance balance = new InventoryBalance();
        balance.setProduct(mock(Product.class));
        balance.setLocation(location);
        balance.setQuantityAvailable(15);

        when(locationRepository.findByIdWithWarehouse(1L)).thenReturn(Optional.of(location));
        when(balanceRepository.findAllByLocationIdWithDetails(null)).thenReturn(List.of(balance));
        when(stockCountRepository.save(any(StockCount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockCountForm form = new StockCountForm();
        form.setLocationId(1L);
        form.setCountDate(LocalDate.now());
        StockCount created = service.create(form);

        assertThat(created.getItems()).hasSize(1);
        assertThat(created.getItems().get(0).getSystemQuantity()).isEqualTo(15);
        assertThat(created.getStatus()).isEqualTo(StockCountStatus.DRAFT);
    }

    @Test
    void submitCreatesPendingAdjustmentForVariance() {
        InventoryBalance balance = new InventoryBalance();
        balance.setQuantityAvailable(10);
        StockCount count = new StockCount();
        StockCountItem item = new StockCountItem();
        item.setStockCount(count);
        item.setInventoryBalance(balance);
        item.setSystemQuantity(10);
        item.setCountedQuantity(8);
        item.setVarianceNote("Two damaged items found");
        count.getItems().add(item);
        when(stockCountRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(count));

        service.submit(2L);

        ArgumentCaptor<StockAdjustment> captor = ArgumentCaptor.forClass(StockAdjustment.class);
        verify(adjustmentRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityChange()).isEqualTo(-2);
        assertThat(captor.getValue().getStatus()).isEqualTo(AdjustmentStatus.PENDING);
        assertThat(count.getStatus()).isEqualTo(StockCountStatus.SUBMITTED);
    }
}
