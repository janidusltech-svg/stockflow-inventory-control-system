package com.stockflow.shared.reporting;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import org.springframework.stereotype.Component;

import static com.stockflow.shared.reporting.CsvReportSupport.DATE;
import static com.stockflow.shared.reporting.CsvReportSupport.row;
import static com.stockflow.shared.reporting.CsvReportSupport.utf8;

@Component
public class StockCsvReportStrategy implements CsvReportStrategy {
    private final InventoryBalanceRepository inventoryBalanceRepository;

    public StockCsvReportStrategy(InventoryBalanceRepository inventoryBalanceRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
    }

    @Override
    public ReportType type() {
        return ReportType.STOCK;
    }

    @Override
    public byte[] generate() {
        StringBuilder csv = new StringBuilder();
        row(csv, "SKU", "Product", "Category", "Brand", "Unit", "Warehouse",
                "Location", "Batch", "Expiry Date", "Available Quantity");
        for (InventoryBalance balance : inventoryBalanceRepository.findAllWithDetails()) {
            row(csv,
                    balance.getProduct().getSku(),
                    balance.getProduct().getName(),
                    balance.getProduct().getCategory().getName(),
                    balance.getProduct().getBrand() == null ? "" : balance.getProduct().getBrand().getName(),
                    balance.getProduct().getUnit().getCode(),
                    balance.getLocation().getWarehouse().getName(),
                    balance.getLocation().getCode() + " - " + balance.getLocation().getName(),
                    balance.getBatch() == null ? "" : balance.getBatch().getBatchNumber(),
                    balance.getBatch() == null || balance.getBatch().getExpiryDate() == null
                            ? "" : DATE.format(balance.getBatch().getExpiryDate()),
                    balance.getQuantityAvailable());
        }
        return utf8(csv);
    }
}
