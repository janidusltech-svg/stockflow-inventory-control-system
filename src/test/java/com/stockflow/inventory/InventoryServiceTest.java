package com.stockflow.inventory;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class InventoryServiceTest {
    private InventoryBalanceRepository inventoryBalanceRepository;
    private StockMovementRepository stockMovementRepository;
    private InventoryService inventoryService;
    @BeforeEach
    void setUp() {
        inventoryBalanceRepository = mock(InventoryBalanceRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        inventoryService = new InventoryService(
                inventoryBalanceRepository,
                stockMovementRepository
        );
    }
    @Test
    void receivedStockIncreasesBalanceAndCreatesMovement() {
        Product product = mock(Product.class);
        StorageLocation location = mock(StorageLocation.class);
        when(inventoryBalanceRepository
                .findByProductAndLocationAndBatchIsNull(product, location))
                .thenReturn(Optional.empty());
        when(inventoryBalanceRepository.save(any(InventoryBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        inventoryService.receive(product, location, null, 12, "GRN-2026-0001");
        ArgumentCaptor<InventoryBalance> balanceCaptor =
                ArgumentCaptor.forClass(InventoryBalance.class);
        verify(inventoryBalanceRepository).save(balanceCaptor.capture());
        assertThat(balanceCaptor.getValue().getQuantityAvailable()).isEqualTo(12);
        ArgumentCaptor<StockMovement> movementCaptor =
                ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType())
                .isEqualTo(StockMovementType.GOODS_RECEIPT);
        assertThat(movementCaptor.getValue().getQuantityChange()).isEqualTo(12);
        assertThat(movementCaptor.getValue().getReferenceNumber())
                .isEqualTo("GRN-2026-0001");
    }
}
