package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.StockMovement;
import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.inventory.StockMovementType;
import com.stockflow.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class InventoryControlService {

    private final ProductRepository productRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReorderRuleService reorderRuleService;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockCountRepository stockCountRepository;

    public InventoryControlService(ProductRepository productRepository,
                                   InventoryBalanceRepository inventoryBalanceRepository,
                                   StockMovementRepository stockMovementRepository,
                                   ReorderRuleService reorderRuleService,
                                   StockAdjustmentRepository stockAdjustmentRepository,
                                   StockCountRepository stockCountRepository) {
        this.productRepository = productRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.reorderRuleService = reorderRuleService;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.stockCountRepository = stockCountRepository;
    }

    public DashboardSummary getSummary() {
        List<ReorderAlert> alerts = reorderRuleService.calculateAlerts();
        long lowStock = alerts.stream().filter(ReorderAlert::isLowStock).count();
        long outOfStock = alerts.stream().filter(ReorderAlert::isOutOfStock).count();
        long totalStock = inventoryBalanceRepository.sumAvailableQuantity();
        long expiring = findExpiringBatches().size();
        long openCounts = stockCountRepository.countByStatus(StockCountStatus.DRAFT)
                + stockCountRepository.countByStatus(StockCountStatus.SUBMITTED);

        return new DashboardSummary(
                productRepository.countByActiveTrue(),
                totalStock,
                lowStock,
                outOfStock,
                expiring,
                stockAdjustmentRepository.countByStatus(AdjustmentStatus.PENDING),
                openCounts
        );
    }

    public List<ReorderAlert> findPriorityAlerts() {
        return reorderRuleService.calculateAlerts().stream()
                .filter(alert -> alert.isOutOfStock() || alert.isLowStock())
                .limit(8)
                .toList();
    }

    public List<InventoryBalance> findExpiringBatches() {
        LocalDate today = LocalDate.now();
        return inventoryBalanceRepository.findExpiringWithDetails(today, today.plusDays(30));
    }

    public List<InventoryBalance> findStock(String keyword) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return inventoryBalanceRepository.findAllWithDetails().stream()
                .filter(balance -> key.isBlank()
                        || contains(balance.getProduct().getSku(), key)
                        || contains(balance.getProduct().getName(), key)
                        || contains(balance.getLocation().getCode(), key)
                        || contains(balance.getLocation().getWarehouse().getName(), key)
                        || (balance.getBatch() != null && contains(balance.getBatch().getBatchNumber(), key)))
                .toList();
    }

    public List<StockMovement> findMovements(StockMovementType movementType, String keyword) {
        String key = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return stockMovementRepository.findAllWithDetailsNewestFirst().stream()
                .filter(movement -> movementType == null || movement.getMovementType() == movementType)
                .filter(movement -> key.isBlank()
                        || contains(movement.getReferenceNumber(), key)
                        || contains(movement.getProduct().getSku(), key)
                        || contains(movement.getProduct().getName(), key)
                        || contains(movement.getLocation().getCode(), key))
                .toList();
    }

    public List<StockMovement> findRecentMovements() {
        return stockMovementRepository.findAllWithDetailsNewestFirst().stream().limit(8).toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
