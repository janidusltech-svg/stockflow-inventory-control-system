package com.stockflow.shared.reporting;

import com.stockflow.receiving.GoodsReceipt;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.springframework.stereotype.Component;

import static com.stockflow.shared.reporting.CsvReportSupport.format;
import static com.stockflow.shared.reporting.CsvReportSupport.readable;
import static com.stockflow.shared.reporting.CsvReportSupport.row;
import static com.stockflow.shared.reporting.CsvReportSupport.utf8;

@Component
public class GoodsReceiptsCsvReportStrategy implements CsvReportStrategy {
    private final GoodsReceiptRepository goodsReceiptRepository;

    public GoodsReceiptsCsvReportStrategy(GoodsReceiptRepository goodsReceiptRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    @Override
    public ReportType type() {
        return ReportType.GOODS_RECEIPTS;
    }

    @Override
    public byte[] generate() {
        StringBuilder csv = new StringBuilder();
        row(csv, "Goods Receipt", "Purchase Order", "Supplier", "Received Date",
                "Warehouse", "Location", "Status", "Delivered", "Accepted", "Rejected");
        for (GoodsReceipt receipt : goodsReceiptRepository.findAllWithDetails()) {
            row(csv,
                    receipt.getReceiptNumber(),
                    receipt.getPurchaseOrder().getOrderNumber(),
                    receipt.getPurchaseOrder().getSupplier().getName(),
                    format(receipt.getReceivedDate()),
                    receipt.getLocation().getWarehouse().getName(),
                    receipt.getLocation().getCode() + " - " + receipt.getLocation().getName(),
                    readable(receipt.getStatus().name()),
                    receipt.getTotalDeliveredQuantity(),
                    receipt.getTotalAcceptedQuantity(),
                    receipt.getTotalRejectedQuantity());
        }
        return utf8(csv);
    }
}
