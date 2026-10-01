package com.stockflow.shared.reporting;

import com.stockflow.control.ReorderRuleService;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.product.ProductRepository;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvReportStrategyTest {
    private final InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
    private final StockMovementRepository movements = mock(StockMovementRepository.class);
    private final PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    private final GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);

    @Test
    void allExistingDownloadsUseTheMatchingStrategyAndKeepTheirHeaders() {
        when(balances.findAllWithDetails()).thenReturn(List.of());
        when(movements.findAllWithDetailsNewestFirst()).thenReturn(List.of());
        when(orders.findAllWithDetails()).thenReturn(List.of());
        when(receipts.findAllWithDetails()).thenReturn(List.of());

        ReportingService service = service(List.of(
                new StockCsvReportStrategy(balances),
                new MovementsCsvReportStrategy(movements),
                new PurchaseOrdersCsvReportStrategy(orders),
                new GoodsReceiptsCsvReportStrategy(receipts)));

        assertThat(text(service.stockCsv())).startsWith("\uFEFF\"SKU\",\"Product\",\"Category\"");
        assertThat(text(service.movementsCsv())).startsWith("\uFEFF\"Date and Time\",\"Movement Type\"");
        assertThat(text(service.purchaseOrdersCsv())).startsWith("\uFEFF\"Purchase Order\",\"Supplier Code\"");
        assertThat(text(service.goodsReceiptsCsv())).startsWith("\uFEFF\"Goods Receipt\",\"Purchase Order\"");
    }

    @Test
    void csvEscapingRemainsSafeForQuotesAndSpreadsheetFormulas() {
        StringBuilder csv = new StringBuilder();
        CsvReportSupport.row(csv, "=SUM(1,1)", "A\"B", null);
        assertThat(csv.toString()).isEqualTo("\"'=SUM(1,1)\",\"A\"\"B\",\"\"\r\n");
    }

    @Test
    void incompleteOrDuplicateStrategyRegistrationsFailAtStartup() {
        CsvReportStrategy stock = new StockCsvReportStrategy(balances);
        assertThatThrownBy(() -> service(List.of(stock)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Missing CSV report strategy");
        assertThatThrownBy(() -> service(List.of(stock, stock)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Duplicate CSV report strategy");
    }

    private ReportingService service(List<CsvReportStrategy> strategies) {
        return new ReportingService(mock(ProductRepository.class), mock(SupplierRepository.class), orders,
                receipts, balances, movements, mock(ReorderRuleService.class), strategies);
    }

    private String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
