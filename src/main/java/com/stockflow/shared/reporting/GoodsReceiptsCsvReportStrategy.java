package com.stockflow.shared.reporting;

import com.stockflow.receiving.GoodsReceipt;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptsCsvReportStrategy implements CsvReportStrategy {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final CsvReportSupport csv;

    public GoodsReceiptsCsvReportStrategy(GoodsReceiptRepository goodsReceiptRepository,
                                          CsvReportSupport csv) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.csv = csv;
    }

    @Override
    public ReportType reportType() {
        return ReportType.GOODS_RECEIPTS;
    }

    @Override
    public byte[] generate() {
        StringBuilder output = new StringBuilder();
        csv.row(output, "Goods Receipt", "Purchase Order", "Supplier", "Received Date",
                "Warehouse", "Location", "Status", "Delivered", "Accepted", "Rejected");
        for (GoodsReceipt receipt : goodsReceiptRepository.findAllWithDetails()) {
            csv.row(output,
                    receipt.getReceiptNumber(),
                    receipt.getPurchaseOrder().getOrderNumber(),
                    receipt.getPurchaseOrder().getSupplier().getName(),
                    csv.format(receipt.getReceivedDate()),
                    receipt.getLocation().getWarehouse().getName(),
                    receipt.getLocation().getCode() + " - " + receipt.getLocation().getName(),
                    csv.readable(receipt.getStatus().name()),
                    receipt.getTotalDeliveredQuantity(),
                    receipt.getTotalAcceptedQuantity(),
                    receipt.getTotalRejectedQuantity());
        }
        return csv.utf8(output);
    }
}
