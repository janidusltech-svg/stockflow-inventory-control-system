package com.stockflow.control;

import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class StockAdjustmentService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryService inventoryService;

    public StockAdjustmentService(StockAdjustmentRepository stockAdjustmentRepository,
                                  InventoryBalanceRepository inventoryBalanceRepository,
                                  InventoryService inventoryService) {
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<StockAdjustment> findAll(AdjustmentStatus status) {
        return stockAdjustmentRepository.findAllWithDetails().stream()
                .filter(adjustment -> status == null || adjustment.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockAdjustment findById(Long id) {
        return stockAdjustmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock adjustment not found"));
    }

    @Transactional(readOnly = true)
    public List<InventoryBalance> findBalances() {
        return inventoryBalanceRepository.findAllWithDetails();
    }

    public StockAdjustment create(StockAdjustmentForm form) {
        validateQuantity(form.getQuantityChange());
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setAdjustmentNumber(nextNumber());
        adjustment.setStatus(AdjustmentStatus.DRAFT);
        applyForm(adjustment, form);
        return stockAdjustmentRepository.save(adjustment);
    }

    public StockAdjustment update(Long id, StockAdjustmentForm form) {
        StockAdjustment adjustment = findById(id);
        requireEditable(adjustment);
        validateQuantity(form.getQuantityChange());
        applyForm(adjustment, form);
        return stockAdjustmentRepository.save(adjustment);
    }

    public void submit(Long id) {
        StockAdjustment adjustment = findById(id);
        requireEditable(adjustment);
        adjustment.setStatus(AdjustmentStatus.PENDING);
        adjustment.setSubmittedAt(LocalDateTime.now());
        stockAdjustmentRepository.save(adjustment);
    }

    public void approve(Long id) {
        StockAdjustment adjustment = findById(id);
        if (!adjustment.isReviewable()) {
            throw new IllegalArgumentException("Only pending adjustments can be approved");
        }

        InventoryBalance balance = adjustment.getInventoryBalance();
        inventoryService.adjust(
                balance.getProduct(),
                balance.getLocation(),
                balance.getBatch(),
                adjustment.getQuantityChange(),
                adjustment.getAdjustmentNumber()
        );
        adjustment.setStatus(AdjustmentStatus.APPLIED);
        adjustment.setReviewedAt(LocalDateTime.now());
        stockAdjustmentRepository.save(adjustment);
    }

    public void reject(Long id) {
        StockAdjustment adjustment = findById(id);
        if (!adjustment.isReviewable()) {
            throw new IllegalArgumentException("Only pending adjustments can be rejected");
        }
        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setReviewedAt(LocalDateTime.now());
        stockAdjustmentRepository.save(adjustment);
    }

    public void deleteDraft(Long id) {
        StockAdjustment adjustment = findById(id);
        requireEditable(adjustment);
        stockAdjustmentRepository.delete(adjustment);
    }

    @Transactional(readOnly = true)
    public StockAdjustmentForm toForm(StockAdjustment adjustment) {
        StockAdjustmentForm form = new StockAdjustmentForm();
        form.setInventoryBalanceId(adjustment.getInventoryBalance().getId());
        form.setQuantityChange(adjustment.getQuantityChange());
        form.setReason(adjustment.getReason());
        form.setNotes(adjustment.getNotes());
        return form;
    }

    private void applyForm(StockAdjustment adjustment, StockAdjustmentForm form) {
        InventoryBalance balance = inventoryBalanceRepository
                .findByIdWithDetails(form.getInventoryBalanceId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory balance not found"));
        if (form.getQuantityChange() < 0
                && balance.getQuantityAvailable() < Math.abs(form.getQuantityChange())) {
            throw new IllegalArgumentException("The negative adjustment exceeds available stock");
        }
        adjustment.setInventoryBalance(balance);
        adjustment.setQuantityChange(form.getQuantityChange());
        adjustment.setReason(form.getReason());
        adjustment.setNotes(form.getNotes());
    }

    private void validateQuantity(Integer quantityChange) {
        if (quantityChange == null || quantityChange == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero");
        }
    }

    private void requireEditable(StockAdjustment adjustment) {
        if (!adjustment.isEditable()) {
            throw new IllegalArgumentException("Only draft adjustments can be changed");
        }
    }

    private String nextNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long sequence = stockAdjustmentRepository.count() + 1;
        return "ADJ-" + date + "-" + String.format("%04d", sequence);
    }
}
