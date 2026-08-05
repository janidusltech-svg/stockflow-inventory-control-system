package com.stockflow.transfer;

import com.stockflow.inventory.Batch;
import com.stockflow.inventory.InventoryBalance;
import com.stockflow.inventory.InventoryBalanceRepository;
import com.stockflow.inventory.InventoryService;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryService inventoryService;

    public StockTransferService(StockTransferRepository stockTransferRepository,
                                StorageLocationRepository storageLocationRepository,
                                InventoryBalanceRepository inventoryBalanceRepository,
                                InventoryService inventoryService) {
        this.stockTransferRepository = stockTransferRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<StockTransfer> findAll(String keyword, StockTransferStatus status) {
        String cleanedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return stockTransferRepository.findAllWithDetails().stream()
                .filter(transfer -> cleanedKeyword.isBlank()
                        || transfer.getTransferNumber().toLowerCase(Locale.ROOT)
                        .contains(cleanedKeyword)
                        || transfer.getSourceLocation().getDisplayName()
                        .toLowerCase(Locale.ROOT).contains(cleanedKeyword)
                        || transfer.getDestinationLocation().getDisplayName()
                        .toLowerCase(Locale.ROOT).contains(cleanedKeyword))
                .filter(transfer -> status == null || transfer.getStatus() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockTransfer findById(Long id) {
        return stockTransferRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock transfer not found"));
    }

    @Transactional(readOnly = true)
    public List<StorageLocation> findActiveLocations() {
        return storageLocationRepository.findAllActiveWithWarehouse();
    }

    @Transactional(readOnly = true)
    public List<InventoryBalance> findAvailableSourceStock(Long locationId) {
        if (locationId == null) {
            return List.of();
        }
        return inventoryBalanceRepository.findAvailableByLocationId(locationId);
    }

    public StockTransfer create(StockTransferForm form) {
        StorageLocation source = requireActiveLocation(form.getSourceLocationId());
        StorageLocation destination = requireActiveLocation(form.getDestinationLocationId());
        validateDifferentLocations(source, destination);
        validateDate(form.getTransferDate());

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(generateTransferNumber());
        applyForm(transfer, form, source, destination);
        return stockTransferRepository.save(transfer);
    }

    public StockTransfer update(Long id, StockTransferForm form) {
        StockTransfer transfer = findById(id);
        requireDraft(transfer);

        StorageLocation source = requireActiveLocation(form.getSourceLocationId());
        StorageLocation destination = requireActiveLocation(form.getDestinationLocationId());
        validateDifferentLocations(source, destination);
        validateDate(form.getTransferDate());
        if (!transfer.getItems().isEmpty()
                && !transfer.getSourceLocation().getId().equals(source.getId())) {
            throw new IllegalArgumentException(
                    "Remove all transfer items before changing the source location"
            );
        }

        applyForm(transfer, form, source, destination);
        return stockTransferRepository.save(transfer);
    }

    public void addItem(Long transferId, StockTransferItemForm form) {
        StockTransfer transfer = findById(transferId);
        requireDraft(transfer);

        InventoryBalance balance = inventoryBalanceRepository
                .findByIdWithDetails(form.getInventoryBalanceId())
                .orElseThrow(() -> new IllegalArgumentException("Available stock record not found"));
        if (!balance.getLocation().getId().equals(transfer.getSourceLocation().getId())) {
            throw new IllegalArgumentException("Select stock from the transfer's source location");
        }
        if (form.getQuantity() == null || form.getQuantity() <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be greater than zero");
        }
        if (form.getQuantity() > balance.getQuantityAvailable()) {
            throw new IllegalArgumentException(
                    "Transfer quantity cannot exceed the available source quantity of "
                            + balance.getQuantityAvailable()
            );
        }

        Long selectedBatchId = idOf(balance.getBatch());
        boolean duplicate = transfer.getItems().stream().anyMatch(item ->
                item.getProduct().getId().equals(balance.getProduct().getId())
                        && Objects.equals(idOf(item.getBatch()), selectedBatchId)
        );
        if (duplicate) {
            throw new IllegalArgumentException(
                    "This product and batch are already included in the transfer"
            );
        }

        StockTransferItem item = new StockTransferItem();
        item.setStockTransfer(transfer);
        item.setProduct(balance.getProduct());
        item.setBatch(balance.getBatch());
        item.setQuantity(form.getQuantity());
        transfer.getItems().add(item);
        stockTransferRepository.save(transfer);
    }

    public void removeItem(Long transferId, Long itemId) {
        StockTransfer transfer = findById(transferId);
        requireDraft(transfer);
        boolean removed = transfer.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Transfer item not found");
        }
        stockTransferRepository.save(transfer);
    }

    public void approve(Long id) {
        StockTransfer transfer = findById(id);
        requireDraft(transfer);
        if (transfer.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one stock item before approval");
        }
        validateCurrentStock(transfer);
        transfer.setStatus(StockTransferStatus.APPROVED);
        stockTransferRepository.save(transfer);
    }

    public void dispatch(Long id) {
        StockTransfer transfer = findById(id);
        if (!transfer.canDispatch()) {
            throw new IllegalArgumentException("Only an approved transfer can be dispatched");
        }
        requireActiveLocation(transfer.getSourceLocation().getId());
        requireActiveLocation(transfer.getDestinationLocation().getId());
        validateCurrentStock(transfer);
        for (StockTransferItem item : transfer.getItems()) {
            inventoryService.transferOut(
                    item.getProduct(),
                    transfer.getSourceLocation(),
                    item.getBatch(),
                    item.getQuantity(),
                    transfer.getTransferNumber()
            );
        }
        transfer.setStatus(StockTransferStatus.DISPATCHED);
        transfer.setDispatchedAt(LocalDateTime.now());
        stockTransferRepository.save(transfer);
    }

    public void receive(Long id) {
        StockTransfer transfer = findById(id);
        if (!transfer.canReceive()) {
            throw new IllegalArgumentException("Only a dispatched transfer can be received");
        }
        requireActiveLocation(transfer.getDestinationLocation().getId());
        for (StockTransferItem item : transfer.getItems()) {
            inventoryService.transferIn(
                    item.getProduct(),
                    transfer.getDestinationLocation(),
                    item.getBatch(),
                    item.getQuantity(),
                    transfer.getTransferNumber()
            );
        }
        transfer.setStatus(StockTransferStatus.RECEIVED);
        transfer.setReceivedAt(LocalDateTime.now());
        stockTransferRepository.save(transfer);
    }

    public void cancel(Long id) {
        StockTransfer transfer = findById(id);
        if (!transfer.canCancel()) {
            throw new IllegalArgumentException(
                    "A dispatched or received transfer cannot be cancelled"
            );
        }
        transfer.setStatus(StockTransferStatus.CANCELLED);
        stockTransferRepository.save(transfer);
    }

    public void deleteDraft(Long id) {
        StockTransfer transfer = findById(id);
        requireDraft(transfer);
        stockTransferRepository.delete(transfer);
    }

    public StockTransferForm toForm(StockTransfer transfer) {
        StockTransferForm form = new StockTransferForm();
        form.setSourceLocationId(transfer.getSourceLocation().getId());
        form.setDestinationLocationId(transfer.getDestinationLocation().getId());
        form.setTransferDate(transfer.getTransferDate());
        form.setNotes(transfer.getNotes());
        return form;
    }

    private void validateCurrentStock(StockTransfer transfer) {
        for (StockTransferItem item : transfer.getItems()) {
            InventoryBalance balance = item.getBatch() == null
                    ? inventoryBalanceRepository.findByProductAndLocationAndBatchIsNull(
                            item.getProduct(), transfer.getSourceLocation())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source stock no longer exists for " + item.getProduct().getName()
                    ))
                    : inventoryBalanceRepository.findByProductAndLocationAndBatch(
                            item.getProduct(), transfer.getSourceLocation(), item.getBatch())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source batch stock no longer exists for "
                                    + item.getProduct().getName()
                    ));
            if (balance.getQuantityAvailable() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient source stock for " + item.getProduct().getName()
                                + ". Available: " + balance.getQuantityAvailable()
                );
            }
        }
    }

    private StorageLocation requireActiveLocation(Long id) {
        StorageLocation location = storageLocationRepository.findByIdWithWarehouse(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage location not found"));
        if (!location.isActive() || !location.getWarehouse().isActive()) {
            throw new IllegalArgumentException("Select an active storage location");
        }
        return location;
    }

    private void validateDifferentLocations(StorageLocation source,
                                            StorageLocation destination) {
        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException(
                    "Source and destination locations must be different"
            );
        }
    }

    private void validateDate(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transfer date cannot be in the future");
        }
    }

    private void applyForm(StockTransfer transfer,
                           StockTransferForm form,
                           StorageLocation source,
                           StorageLocation destination) {
        transfer.setSourceLocation(source);
        transfer.setDestinationLocation(destination);
        transfer.setTransferDate(form.getTransferDate());
        transfer.setNotes(form.getNotes());
    }

    private void requireDraft(StockTransfer transfer) {
        if (!transfer.isEditable()) {
            throw new IllegalArgumentException("Only draft transfers can be changed");
        }
    }

    private Long idOf(Batch batch) {
        return batch == null ? null : batch.getId();
    }

    private String generateTransferNumber() {
        long nextNumber = stockTransferRepository.findTopByOrderByIdDesc()
                .map(transfer -> transfer.getId() + 1)
                .orElse(1L);
        return "TRF-" + LocalDate.now().getYear() + "-"
                + String.format("%04d", nextNumber);
    }
}
