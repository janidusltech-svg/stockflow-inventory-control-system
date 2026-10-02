package com.stockflow.shared.reporting;

import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderRepository;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrdersCsvReportStrategy implements CsvReportStrategy {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CsvReportSupport csv;

    public PurchaseOrdersCsvReportStrategy(PurchaseOrderRepository purchaseOrderRepository,
                                           CsvReportSupport csv) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.csv = csv;
    }

    @Override
    public ReportType reportType() {
        return ReportType.PURCHASE_ORDERS;
    }

    @Override
    public byte[] generate() {
        StringBuilder output = new StringBuilder();
        csv.row(output, "Purchase Order", "Supplier Code", "Supplier", "Order Date",
                "Expected Date", "Status", "Total Amount");
        for (PurchaseOrder order : purchaseOrderRepository.findAllWithDetails()) {
            csv.row(output,
                    order.getOrderNumber(),
                    order.getSupplier().getSupplierCode(),
                    order.getSupplier().getName(),
                    csv.format(order.getOrderDate()),
                    csv.format(order.getExpectedDate()),
                    csv.readable(order.getStatus().name()),
                    csv.money(order.getTotalAmount()));
        }
        return csv.utf8(output);
    }
}
