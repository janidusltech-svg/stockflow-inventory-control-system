package com.stockflow.shared.reporting;

import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderRepository;
import org.springframework.stereotype.Component;

import static com.stockflow.shared.reporting.CsvReportSupport.format;
import static com.stockflow.shared.reporting.CsvReportSupport.money;
import static com.stockflow.shared.reporting.CsvReportSupport.readable;
import static com.stockflow.shared.reporting.CsvReportSupport.row;
import static com.stockflow.shared.reporting.CsvReportSupport.utf8;

@Component
public class PurchaseOrdersCsvReportStrategy implements CsvReportStrategy {
    private final PurchaseOrderRepository purchaseOrderRepository;

    public PurchaseOrdersCsvReportStrategy(PurchaseOrderRepository purchaseOrderRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Override
    public ReportType type() {
        return ReportType.PURCHASE_ORDERS;
    }

    @Override
    public byte[] generate() {
        StringBuilder csv = new StringBuilder();
        row(csv, "Purchase Order", "Supplier Code", "Supplier", "Order Date",
                "Expected Date", "Status", "Total Amount");
        for (PurchaseOrder order : purchaseOrderRepository.findAllWithDetails()) {
            row(csv,
                    order.getOrderNumber(),
                    order.getSupplier().getSupplierCode(),
                    order.getSupplier().getName(),
                    format(order.getOrderDate()),
                    format(order.getExpectedDate()),
                    readable(order.getStatus().name()),
                    money(order.getTotalAmount()));
        }
        return utf8(csv);
    }
}
