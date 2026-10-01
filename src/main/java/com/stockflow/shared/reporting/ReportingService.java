package com.stockflow.shared.reporting;

import com.stockflow.control.ReorderAlert;
import com.stockflow.control.ReorderRuleService;
import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.inventory.StockMovementType;
import com.stockflow.product.ProductRepository;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.receiving.GoodsReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReorderRuleService reorderRuleService;
    private final Map<ReportType, CsvReportStrategy> csvStrategies;

    public ReportingService(ProductRepository productRepository,
                            SupplierRepository supplierRepository,
                            PurchaseOrderRepository purchaseOrderRepository,
                            GoodsReceiptRepository goodsReceiptRepository,
                            InventoryBalanceRepository inventoryBalanceRepository,
                            StockMovementRepository stockMovementRepository,
                            ReorderRuleService reorderRuleService,
                            List<CsvReportStrategy> csvStrategies) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.reorderRuleService = reorderRuleService;
        Map<ReportType, CsvReportStrategy> selected = new EnumMap<>(ReportType.class);
        for (CsvReportStrategy strategy : csvStrategies) {
            if (selected.putIfAbsent(strategy.type(), strategy) != null) {
                throw new IllegalStateException("Duplicate CSV report strategy: " + strategy.type());
            }
        }
        for (ReportType type : ReportType.values()) {
            if (!selected.containsKey(type)) {
                throw new IllegalStateException("Missing CSV report strategy: " + type);
            }
        }
        this.csvStrategies = Map.copyOf(selected);
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
        return export(ReportType.STOCK);
    }

    public byte[] movementsCsv() {
        return export(ReportType.MOVEMENTS);
    }

    public byte[] purchaseOrdersCsv() {
        return export(ReportType.PURCHASE_ORDERS);
    }

    public byte[] goodsReceiptsCsv() {
        return export(ReportType.GOODS_RECEIPTS);
    }

    public byte[] export(ReportType type) {
        CsvReportStrategy strategy = csvStrategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported CSV report: " + type);
        }
        return strategy.generate();
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

    private String readable(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

}
