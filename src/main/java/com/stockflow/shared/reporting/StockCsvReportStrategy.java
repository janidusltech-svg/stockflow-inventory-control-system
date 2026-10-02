package com.stockflow.shared.reporting;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import org.springframework.stereotype.Component;

@Component
public class StockCsvReportStrategy implements CsvReportStrategy {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final CsvReportSupport csv;

    public StockCsvReportStrategy(InventoryBalanceRepository inventoryBalanceRepository,
                                  CsvReportSupport csv) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.csv = csv;
    }

    @Override
    public ReportType reportType() {
        return ReportType.STOCK;
    }

    @Override
    public byte[] generate() {
        StringBuilder output = new StringBuilder();
        csv.row(output, "SKU", "Product", "Category", "Brand", "Unit", "Warehouse",
                "Location", "Batch", "Expiry Date", "Available Quantity");
        for (InventoryBalance balance : inventoryBalanceRepository.findAllWithDetails()) {
            csv.row(output,
                    balance.getProduct().getSku(),
                    balance.getProduct().getName(),
                    balance.getProduct().getCategory().getName(),
                    balance.getProduct().getBrand() == null ? "" : balance.getProduct().getBrand().getName(),
                    balance.getProduct().getUnit().getCode(),
                    balance.getLocation().getWarehouse().getName(),
                    balance.getLocation().getCode() + " - " + balance.getLocation().getName(),
                    balance.getBatch() == null ? "" : balance.getBatch().getBatchNumber(),
                    balance.getBatch() == null ? "" : csv.format(balance.getBatch().getExpiryDate()),
                    balance.getQuantityAvailable());
        }
        return csv.utf8(output);
    }
}
