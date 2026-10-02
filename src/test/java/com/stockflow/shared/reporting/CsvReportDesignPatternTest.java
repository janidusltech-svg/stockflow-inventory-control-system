package com.stockflow.shared.reporting;

import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvReportDesignPatternTest {

    @Test
    void factorySelectsTheStrategyRegisteredForEachReportType() {
        CsvReportStrategy stock = strategy(ReportType.STOCK, "stock");
        CsvReportStrategy movements = strategy(ReportType.MOVEMENTS, "movements");
        CsvReportStrategy orders = strategy(ReportType.PURCHASE_ORDERS, "orders");
        CsvReportStrategy receipts = strategy(ReportType.GOODS_RECEIPTS, "receipts");

        CsvReportStrategyFactory factory = new CsvReportStrategyFactory(
                List.of(stock, movements, orders, receipts));

        assertThat(factory.getStrategy(ReportType.STOCK)).isSameAs(stock);
        assertThat(factory.getStrategy(ReportType.MOVEMENTS)).isSameAs(movements);
        assertThat(factory.getStrategy(ReportType.PURCHASE_ORDERS)).isSameAs(orders);
        assertThat(factory.getStrategy(ReportType.GOODS_RECEIPTS)).isSameAs(receipts);
        assertThat(new String(factory.generate(ReportType.STOCK), StandardCharsets.UTF_8))
                .isEqualTo("stock");
    }

    @Test
    void factoryFailsFastWhenARequiredStrategyIsMissing() {
        assertThatThrownBy(() -> new CsvReportStrategyFactory(List.of(
                strategy(ReportType.STOCK, "stock"),
                strategy(ReportType.MOVEMENTS, "movements"),
                strategy(ReportType.PURCHASE_ORDERS, "orders"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GOODS_RECEIPTS");
    }

    @Test
    void everyConcreteStrategyProducesItsOriginalCsvHeader() {
        CsvReportSupport support = new CsvReportSupport();

        InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
        when(balances.findAllWithDetails()).thenReturn(List.of());
        StockMovementRepository movements = mock(StockMovementRepository.class);
        when(movements.findAllWithDetailsNewestFirst()).thenReturn(List.of());
        PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
        when(orders.findAllWithDetails()).thenReturn(List.of());
        GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
        when(receipts.findAllWithDetails()).thenReturn(List.of());

        assertThat(text(new StockCsvReportStrategy(balances, support).generate()))
                .contains("SKU", "Available Quantity");
        assertThat(text(new MovementsCsvReportStrategy(movements, support).generate()))
                .contains("Movement Type", "Quantity Change");
        assertThat(text(new PurchaseOrdersCsvReportStrategy(orders, support).generate()))
                .contains("Purchase Order", "Total Amount");
        assertThat(text(new GoodsReceiptsCsvReportStrategy(receipts, support).generate()))
                .contains("Goods Receipt", "Accepted", "Rejected");
    }

    @Test
    void csvSupportProtectsSpreadsheetFormulaValues() {
        CsvReportSupport support = new CsvReportSupport();
        StringBuilder output = new StringBuilder();

        support.row(output, "=2+2", "+123", "normal");

        assertThat(output.toString())
                .isEqualTo("\"'=2+2\",\"'+123\",\"normal\"\r\n");
    }

    private CsvReportStrategy strategy(ReportType type, String output) {
        return new CsvReportStrategy() {
            @Override
            public ReportType reportType() {
                return type;
            }

            @Override
            public byte[] generate() {
                return output.getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    private String text(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
