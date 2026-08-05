package com.stockflow.issuing;

import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockIssueServiceTest {

    private StockIssueRepository stockIssueRepository;
    private InventoryService inventoryService;
    private StockIssueService stockIssueService;

    @BeforeEach
    void setUp() {
        stockIssueRepository = mock(StockIssueRepository.class);
        RecipientRepository recipientRepository = mock(RecipientRepository.class);
        InventoryBalanceRepository inventoryBalanceRepository =
                mock(InventoryBalanceRepository.class);
        inventoryService = mock(InventoryService.class);
        stockIssueService = new StockIssueService(stockIssueRepository, recipientRepository,
                inventoryBalanceRepository, inventoryService);
    }

    @Test
    void confirmsDraftAndDeductsItsProductsFromInventory() {
        Product product = mock(Product.class);
        StorageLocation location = mock(StorageLocation.class);
        StockIssue issue = new StockIssue();
        issue.setIssueNumber("ISS-2026-00001");
        issue.setIssueDate(LocalDate.now());

        StockIssueItem item = new StockIssueItem();
        item.setStockIssue(issue);
        item.setProduct(product);
        item.setLocation(location);
        item.setQuantity(8);
        issue.getItems().add(item);

        when(stockIssueRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(issue));

        stockIssueService.confirm(1L);

        assertThat(issue.getStatus()).isEqualTo(StockIssueStatus.CONFIRMED);
        assertThat(issue.getConfirmedAt()).isNotNull();
        verify(inventoryService).issue(product, location, null, 8, "ISS-2026-00001");
        verify(stockIssueRepository).save(issue);
    }

    @Test
    void rejectsConfirmationWhenDraftHasNoProducts() {
        StockIssue issue = new StockIssue();
        issue.setIssueNumber("ISS-2026-00002");
        issue.setIssueDate(LocalDate.now());
        when(stockIssueRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> stockIssueService.confirm(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one product");
    }
}
