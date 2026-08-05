package com.stockflow.issuing;

import com.stockflow.inventory.InventoryService;
import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockReturnServiceTest {

    private StockReturnRepository stockReturnRepository;
    private StockIssueRepository stockIssueRepository;
    private InventoryService inventoryService;
    private StockReturnService stockReturnService;

    @BeforeEach
    void setUp() {
        stockReturnRepository = mock(StockReturnRepository.class);
        stockIssueRepository = mock(StockIssueRepository.class);
        inventoryService = mock(InventoryService.class);
        stockReturnService = new StockReturnService(
                stockReturnRepository, stockIssueRepository, inventoryService);
    }

    @Test
    void confirmationRestocksOnlyAcceptedQuantityAndTracksRejectedQuantity() {
        Product product = mock(Product.class);
        StorageLocation location = mock(StorageLocation.class);

        StockIssue issue = new StockIssue();
        issue.setIssueNumber("ISS-2026-00001");
        issue.setIssueDate(LocalDate.now().minusDays(1));
        issue.setStatus(StockIssueStatus.CONFIRMED);
        StockIssueItem issueItem = new StockIssueItem();
        issueItem.setStockIssue(issue);
        issueItem.setProduct(product);
        issueItem.setLocation(location);
        issueItem.setQuantity(10);
        issue.getItems().add(issueItem);

        StockReturn returned = new StockReturn();
        returned.setReturnNumber("RTN-2026-00001");
        returned.setStockIssue(issue);
        returned.setReturnDate(LocalDate.now());
        StockReturnItem returnItem = new StockReturnItem();
        returnItem.setStockReturn(returned);
        returnItem.setIssueItem(issueItem);
        returnItem.setReturnedQuantity(4);
        returnItem.setRestockQuantity(3);
        returnItem.setRejectedQuantity(1);
        returnItem.setReturnReason("Customer cancellation");
        returnItem.setConditionNote("One pack damaged");
        returned.getItems().add(returnItem);

        when(stockReturnRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(returned));
        when(stockIssueRepository.findByIdWithDetails(null)).thenReturn(Optional.of(issue));

        stockReturnService.confirm(1L);

        assertThat(returned.getStatus()).isEqualTo(StockReturnStatus.CONFIRMED);
        assertThat(issueItem.getReturnedQuantity()).isEqualTo(4);
        assertThat(issue.getStatus()).isEqualTo(StockIssueStatus.PARTIALLY_RETURNED);
        verify(inventoryService).returnToStock(product, location, null, 3,
                "RTN-2026-00001");
        verify(stockIssueRepository).save(issue);
        verify(stockReturnRepository).save(returned);
    }
}
