package com.stockflow.shared.reporting;

import com.stockflow.inventory.StockMovement;
import com.stockflow.inventory.StockMovementRepository;
import org.springframework.stereotype.Component;

@Component
public class MovementsCsvReportStrategy implements CsvReportStrategy {

    private final StockMovementRepository stockMovementRepository;
    private final CsvReportSupport csv;

    public MovementsCsvReportStrategy(StockMovementRepository stockMovementRepository,
                                      CsvReportSupport csv) {
        this.stockMovementRepository = stockMovementRepository;
        this.csv = csv;
    }

    @Override
    public ReportType reportType() {
        return ReportType.MOVEMENTS;
    }

    @Override
    public byte[] generate() {
        StringBuilder output = new StringBuilder();
        csv.row(output, "Date and Time", "Movement Type", "Reference", "SKU", "Product",
                "Warehouse", "Location", "Batch", "Quantity Change");
        for (StockMovement movement : stockMovementRepository.findAllWithDetailsNewestFirst()) {
            csv.row(output,
                    csv.format(movement.getCreatedAt()),
                    csv.readable(movement.getMovementType().name()),
                    movement.getReferenceNumber(),
                    movement.getProduct().getSku(),
                    movement.getProduct().getName(),
                    movement.getLocation().getWarehouse().getName(),
                    movement.getLocation().getCode() + " - " + movement.getLocation().getName(),
                    movement.getBatch() == null ? "" : movement.getBatch().getBatchNumber(),
                    movement.getQuantityChange());
        }
        return csv.utf8(output);
    }
}
