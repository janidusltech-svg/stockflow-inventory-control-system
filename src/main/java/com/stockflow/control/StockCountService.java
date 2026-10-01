package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class StockCountService {

    private final StockCountRepository stockCountRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    public StockCountService(StockCountRepository stockCountRepository,
                             StorageLocationRepository storageLocationRepository,
                             InventoryBalanceRepository inventoryBalanceRepository,
                             StockAdjustmentRepository stockAdjustmentRepository) {
        this.stockCountRepository = stockCountRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
    }

    @Transactional(readOnly = true)
    public List<StockCount> findAll(StockCountStatus status) {
        return stockCountRepository.findAllWithLocationAndItems().stream()
                .filter(stockCount -> status == null || stockCount.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockCount findById(Long id) {
        return stockCountRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock count session not found"));
    }

    @Transactional(readOnly = true)
    public List<StorageLocation> findLocations() {
        return storageLocationRepository.findAllActiveWithWarehouse();
    }

    public StockCount create(StockCountForm form) {
        StorageLocation location = storageLocationRepository
                .findByIdWithWarehouse(form.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Storage location not found"));
        if (!location.isActive() || !location.getWarehouse().isActive()) {
            throw new IllegalArgumentException("Select an active warehouse location");
        }

        List<InventoryBalance> balances = inventoryBalanceRepository
                .findAllByLocationIdWithDetails(location.getId());
        if (balances.isEmpty()) {
            throw new IllegalArgumentException("No inventory balances exist at this location");
        }

        StockCount stockCount = new StockCount();
        stockCount.setCountNumber(nextNumber());
        stockCount.setLocation(location);
        stockCount.setCountDate(form.getCountDate());
        stockCount.setNotes(form.getNotes());

        for (InventoryBalance balance : balances) {
            StockCountItem item = new StockCountItem();
            item.setStockCount(stockCount);
            item.setInventoryBalance(balance);
            item.setSystemQuantity(balance.getQuantityAvailable());
            stockCount.getItems().add(item);
        }
        return stockCountRepository.save(stockCount);
    }

    public void updateItem(Long stockCountId, Long itemId,
                           Integer countedQuantity, String varianceNote) {
        StockCount stockCount = findById(stockCountId);
        requireEditable(stockCount);
        if (countedQuantity == null || countedQuantity < 0) {
            throw new IllegalArgumentException("Counted quantity cannot be negative");
        }

        StockCountItem item = stockCount.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stock count item not found"));

        if (!countedQuantity.equals(item.getSystemQuantity())
                && (varianceNote == null || varianceNote.isBlank())) {
            throw new IllegalArgumentException("Enter a reason when physical and system quantities differ");
        }
        item.setCountedQuantity(countedQuantity);
        item.setVarianceNote(varianceNote);
        stockCountRepository.save(stockCount);
    }

    public void submit(Long id) {
        StockCount stockCount = findById(id);
        requireEditable(stockCount);
        if (stockCount.getItems().stream().anyMatch(item -> item.getCountedQuantity() == null)) {
            throw new IllegalArgumentException("Record a physical quantity for every stock item before submitting");
        }

        stockCount.setStatus(StockCountStatus.SUBMITTED);
        stockCount.setSubmittedAt(LocalDateTime.now());

        for (StockCountItem item : stockCount.getItems()) {
            if (item.getVariance() != 0) {
                StockAdjustment adjustment = new StockAdjustment();
                adjustment.setAdjustmentNumber(nextAdjustmentNumber());
                adjustment.setInventoryBalance(item.getInventoryBalance());
                adjustment.setStockCount(stockCount);
                adjustment.setQuantityChange(item.getVariance());
                adjustment.setReason("Physical stock-count variance");
                adjustment.setNotes(item.getVarianceNote());
                adjustment.setStatus(AdjustmentStatus.PENDING);
                adjustment.setSubmittedAt(LocalDateTime.now());
                stockAdjustmentRepository.save(adjustment);
            }
        }
        stockCountRepository.save(stockCount);
    }

    public void close(Long id) {
        StockCount stockCount = findById(id);
        if (!stockCount.isSubmitted()) {
            throw new IllegalArgumentException("Only submitted stock counts can be closed");
        }
        long pending = stockAdjustmentRepository
                .countByStockCountIdAndStatus(id, AdjustmentStatus.PENDING);
        if (pending > 0) {
            throw new IllegalArgumentException("Review all generated adjustments before closing this count");
        }
        stockCount.setStatus(StockCountStatus.CLOSED);
        stockCount.setClosedAt(LocalDateTime.now());
        stockCountRepository.save(stockCount);
    }

    public void cancel(Long id) {
        StockCount stockCount = findById(id);
        requireEditable(stockCount);
        stockCount.setStatus(StockCountStatus.CANCELLED);
        stockCountRepository.save(stockCount);
    }

    public void deleteDraft(Long id) {
        StockCount stockCount = findById(id);
        requireEditable(stockCount);
        if (stockAdjustmentRepository.existsByStockCountId(id)) {
            throw new IllegalArgumentException("This stock count has adjustments and cannot be deleted");
        }
        // Only the draft's own count lines are removed; inventory balances are not touched.
        stockCountRepository.delete(stockCount);
    }

    private void requireEditable(StockCount stockCount) {
        if (!stockCount.isEditable()) {
            throw new IllegalArgumentException("Only draft stock counts can be changed");
        }
    }

    // count() + 1 alone can repeat an existing number after a draft is deleted,
    // so skip forward until the number is free.
    private String nextNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long sequence = stockCountRepository.count() + 1;
        String number;
        do {
            number = "CNT-" + date + "-" + String.format("%04d", sequence++);
        } while (stockCountRepository.existsByCountNumber(number));
        return number;
    }

    private String nextAdjustmentNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long sequence = stockAdjustmentRepository.count() + 1;
        String number;
        do {
            number = "ADJ-" + date + "-" + String.format("%04d", sequence++);
        } while (stockAdjustmentRepository.existsByAdjustmentNumber(number));
        return number;
    }
}
