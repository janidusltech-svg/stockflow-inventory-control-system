package com.stockflow.shared.reporting;

import com.stockflow.inventory.StockMovement;
import com.stockflow.inventory.StockMovementRepository;
import org.springframework.stereotype.Component;

import static com.stockflow.shared.reporting.CsvReportSupport.DATE_TIME;
import static com.stockflow.shared.reporting.CsvReportSupport.readable;
import static com.stockflow.shared.reporting.CsvReportSupport.row;
import static com.stockflow.shared.reporting.CsvReportSupport.utf8;

@Component
public class MovementsCsvReportStrategy implements CsvReportStrategy {
    private final StockMovementRepository stockMovementRepository;

    public MovementsCsvReportStrategy(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public ReportType type() {
        return ReportType.MOVEMENTS;
    }

    @Override
    public byte[] generate() {
        StringBuilder csv = new StringBuilder();
        row(csv, "Date and Time", "Movement Type", "Reference", "SKU", "Product",
                "Warehouse", "Location", "Batch", "Quantity Change");
        for (StockMovement movement : stockMovementRepository.findAllWithDetailsNewestFirst()) {
            row(csv,
                    DATE_TIME.format(movement.getCreatedAt()),
                    readable(movement.getMovementType().name()),
                    movement.getReferenceNumber(),
                    movement.getProduct().getSku(),
                    movement.getProduct().getName(),
                    movement.getLocation().getWarehouse().getName(),
                    movement.getLocation().getCode() + " - " + movement.getLocation().getName(),
                    movement.getBatch() == null ? "" : movement.getBatch().getBatchNumber(),
                    movement.getQuantityChange());
        }
        return utf8(csv);
    }
}
