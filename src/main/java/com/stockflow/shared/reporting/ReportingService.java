package com.stockflow.shared.reporting;

import com.stockflow.control.ReorderAlert;
import com.stockflow.control.ReorderRuleService;
import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.StockMovement;
import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.inventory.StockMovementType;
import com.stockflow.product.ProductRepository;
import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.receiving.GoodsReceipt;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReorderRuleService reorderRuleService;

    public ReportingService(ProductRepository productRepository,
                            SupplierRepository supplierRepository,
                            PurchaseOrderRepository purchaseOrderRepository,
                            GoodsReceiptRepository goodsReceiptRepository,
                            InventoryBalanceRepository inventoryBalanceRepository,
                            StockMovementRepository stockMovementRepository,
                            ReorderRuleService reorderRuleService) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.reorderRuleService = reorderRuleService;
    }

    public Map<String, Long> summary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("activeProducts", productRepository.countByActiveTrue());
        summary.put("stockUnits", inventoryBalanceRepository.sumAvailableQuantity());
        summary.put("suppliers", supplierRepository.count());
        summary.put("purchaseOrders", purchaseOrderRepository.count());
        summary.put("goodsReceipts", goodsReceiptRepository.count());
        summary.put("movements", stockMovementRepository.count());
        return summary;
    }

    public List<ChartBar> stockByWarehouse() {
        Map<String, Long> totals = inventoryBalanceRepository.findAllWithDetails().stream()
                .filter(balance -> balance.getQuantityAvailable() > 0)
                .collect(Collectors.groupingBy(
                        balance -> balance.getLocation().getWarehouse().getName(),
                        Collectors.summingLong(InventoryBalance::getQuantityAvailable)
                ));
        return toBars(totals);
    }

    public List<ChartBar> movementActivityLastThirtyDays() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        Map<StockMovementType, Long> counts = new EnumMap<>(StockMovementType.class);
        stockMovementRepository.findAllWithDetailsNewestFirst().stream()
                .filter(movement -> !movement.getCreatedAt().isBefore(from))
                .forEach(movement -> counts.merge(movement.getMovementType(), 1L, Long::sum));
        Map<String, Long> labels = counts.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> readable(entry.getKey().name()),
                        Map.Entry::getValue,
                        Long::sum,
                        LinkedHashMap::new
                ));
        return toBars(labels);
    }

    public List<ChartBar> inventoryHealth() {
        List<ReorderAlert> alerts = reorderRuleService.calculateAlerts();
        long out = alerts.stream().filter(ReorderAlert::isOutOfStock).count();
        long low = alerts.stream().filter(ReorderAlert::isLowStock).count();
        long active = productRepository.countByActiveTrue();
        long normal = Math.max(0, active - out - low);
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("Normal", normal);
        values.put("Low stock", low);
        values.put("Out of stock", out);
        return toBars(values);
    }

    public byte[] stockCsv() {
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

    public byte[] movementsCsv() {
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

    public byte[] purchaseOrdersCsv() {
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

    public byte[] goodsReceiptsCsv() {
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

    private List<ChartBar> toBars(Map<String, Long> values) {
        long maximum = values.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new ChartBar(entry.getKey(), entry.getValue(), percentage(entry.getValue(), maximum)))
                .toList();
    }

    private int percentage(long value, long maximum) {
        if (value <= 0 || maximum <= 0) {
            return 0;
        }
        return Math.max(4, (int) Math.round(value * 100.0 / maximum));
    }

    private void row(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append("\r\n");
    }

    private String escape(Object raw) {
        String value = raw == null ? "" : raw.toString();
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private byte[] utf8(StringBuilder csv) {
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private String readable(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String format(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
