package com.stockflow.receiving;

import com.stockflow.inventory.Batch;
import com.stockflow.inventory.BatchService;
import com.stockflow.inventory.InventoryService;
import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderItem;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.PurchaseOrderStatus;
import com.stockflow.warehouse.StorageLocation;
import com.stockflow.warehouse.StorageLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class GoodsReceiptService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final BatchService batchService;
    private final InventoryService inventoryService;

    public GoodsReceiptService(GoodsReceiptRepository goodsReceiptRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               StorageLocationRepository storageLocationRepository,
                               BatchService batchService,
                               InventoryService inventoryService) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.batchService = batchService;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<GoodsReceipt> findAll(String keyword,
                                      GoodsReceiptStatus status,
                                      LocalDate fromDate,
                                      LocalDate toDate) {
        String cleanedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return goodsReceiptRepository.findAllWithDetails().stream()
                .filter(receipt -> cleanedKeyword.isBlank()
                        || receipt.getReceiptNumber().toLowerCase(Locale.ROOT).contains(cleanedKeyword)
                        || receipt.getPurchaseOrder().getOrderNumber().toLowerCase(Locale.ROOT)
                        .contains(cleanedKeyword)
                        || receipt.getPurchaseOrder().getSupplier().getName().toLowerCase(Locale.ROOT)
                        .contains(cleanedKeyword)
                        || containsIgnoreCase(receipt.getSupplierDeliveryReference(), cleanedKeyword))
                .filter(receipt -> status == null || receipt.getStatus() == status)
                .filter(receipt -> fromDate == null || !receipt.getReceivedDate().isBefore(fromDate))
                .filter(receipt -> toDate == null || !receipt.getReceivedDate().isAfter(toDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoodsReceipt findById(Long id) {
        return goodsReceiptRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Goods receipt not found"));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findReceivablePurchaseOrders() {
        return purchaseOrderRepository.findAllWithDetails().stream()
                .filter(this::isReceivable)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StorageLocation> findActiveLocations() {
        return storageLocationRepository.findAllActiveWithWarehouse();
    }

    public GoodsReceipt create(GoodsReceiptForm form) {
        PurchaseOrder order = requireReceivableOrder(form.getPurchaseOrderId());
        StorageLocation location = requireActiveLocation(form.getLocationId());
        validateReceivedDate(form.getReceivedDate(), order);
        rejectDuplicateDraft(order.getId(), null);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNumber(generateReceiptNumber());
        applyForm(receipt, form, order, location);
        return goodsReceiptRepository.save(receipt);
    }

    public GoodsReceipt update(Long id, GoodsReceiptForm form) {
        GoodsReceipt receipt = findById(id);
        requireDraft(receipt);

        PurchaseOrder order = requireReceivableOrder(form.getPurchaseOrderId());
        if (!receipt.getItems().isEmpty()
                && !receipt.getPurchaseOrder().getId().equals(order.getId())) {
            throw new IllegalArgumentException("Remove all receipt items before changing the purchase order");
        }

        StorageLocation location = requireActiveLocation(form.getLocationId());
        validateReceivedDate(form.getReceivedDate(), order);
        rejectDuplicateDraft(order.getId(), receipt.getId());
        applyForm(receipt, form, order, location);
        return goodsReceiptRepository.save(receipt);
    }

    public void addItem(Long receiptId, GoodsReceiptItemForm form) {
        GoodsReceipt receipt = findById(receiptId);
        requireDraft(receipt);

        PurchaseOrder order = purchaseOrderRepository
                .findByIdWithDetails(receipt.getPurchaseOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        PurchaseOrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getId().equals(form.getPurchaseOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Select a product from the related purchase order"
                ));

        boolean alreadyAdded = receipt.getItems().stream()
                .anyMatch(item -> item.getPurchaseOrderItem().getId().equals(orderItem.getId()));
        if (alreadyAdded) {
            throw new IllegalArgumentException("This purchase-order product is already in the receipt");
        }

        validateItem(form, orderItem, receipt.getReceivedDate());

        GoodsReceiptItem receiptItem = new GoodsReceiptItem();
        receiptItem.setGoodsReceipt(receipt);
        receiptItem.setPurchaseOrderItem(orderItem);
        receiptItem.setProduct(orderItem.getProduct());
        receiptItem.setDeliveredQuantity(form.getDeliveredQuantity());
        receiptItem.setAcceptedQuantity(form.getAcceptedQuantity());
        receiptItem.setRejectedQuantity(form.getRejectedQuantity());
        receiptItem.setRejectionReason(form.getRejectionReason());
        receiptItem.setBatchNumber(form.getBatchNumber());
        receiptItem.setManufactureDate(form.getManufactureDate());
        receiptItem.setExpiryDate(form.getExpiryDate());
        receipt.getItems().add(receiptItem);
        goodsReceiptRepository.save(receipt);
    }

    public void removeItem(Long receiptId, Long itemId) {
        GoodsReceipt receipt = findById(receiptId);
        requireDraft(receipt);
        boolean removed = receipt.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Goods-receipt item not found");
        }
        goodsReceiptRepository.save(receipt);
    }

    public void confirm(Long receiptId) {
        GoodsReceipt receipt = findById(receiptId);
        requireDraft(receipt);
        if (receipt.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one delivered product before confirmation");
        }

        PurchaseOrder order = purchaseOrderRepository
                .findByIdWithDetails(receipt.getPurchaseOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        if (!isReceivable(order)) {
            throw new IllegalArgumentException("The purchase order is not available for receiving");
        }

        for (GoodsReceiptItem receiptItem : receipt.getItems()) {
            PurchaseOrderItem orderItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(receiptItem.getPurchaseOrderItem().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A receipt item no longer belongs to this purchase order"
                    ));

            validateStoredItem(receiptItem, orderItem, receipt.getReceivedDate());

            Batch batch = null;
            if (receiptItem.getAcceptedQuantity() > 0) {
                batch = batchService.resolveBatch(
                        receiptItem.getProduct(),
                        receiptItem.getBatchNumber(),
                        receiptItem.getManufactureDate(),
                        receiptItem.getExpiryDate(),
                        receipt.getReceivedDate()
                );
                receiptItem.setBatch(batch);
                inventoryService.receive(
                        receiptItem.getProduct(),
                        receipt.getLocation(),
                        batch,
                        receiptItem.getAcceptedQuantity(),
                        receipt.getReceiptNumber()
                );
                orderItem.setReceivedQuantity(
                        orderItem.getReceivedQuantity() + receiptItem.getAcceptedQuantity()
                );
            }
        }

        updatePurchaseOrderStatus(order);
        purchaseOrderRepository.save(order);
        receipt.setStatus(GoodsReceiptStatus.CONFIRMED);
        receipt.setConfirmedAt(LocalDateTime.now());
        goodsReceiptRepository.save(receipt);
    }

    public void cancelDraft(Long id) {
        GoodsReceipt receipt = findById(id);
        requireDraft(receipt);
        receipt.setStatus(GoodsReceiptStatus.CANCELLED);
        goodsReceiptRepository.save(receipt);
    }

    public void deleteDraft(Long id) {
        GoodsReceipt receipt = findById(id);
        requireDraft(receipt);
        goodsReceiptRepository.delete(receipt);
    }

    public GoodsReceiptForm toForm(GoodsReceipt receipt) {
        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseOrderId(receipt.getPurchaseOrder().getId());
        form.setLocationId(receipt.getLocation().getId());
        form.setReceivedDate(receipt.getReceivedDate());
        form.setSupplierDeliveryReference(receipt.getSupplierDeliveryReference());
        form.setNotes(receipt.getNotes());
        return form;
    }

    private void validateItem(GoodsReceiptItemForm form,
                              PurchaseOrderItem orderItem,
                              LocalDate receivedDate) {
        int delivered = form.getDeliveredQuantity();
        int accepted = form.getAcceptedQuantity();
        int rejected = form.getRejectedQuantity();

        if (delivered <= 0 || accepted < 0 || rejected < 0) {
            throw new IllegalArgumentException("Receipt quantities cannot be negative or zero-delivered");
        }
        if (accepted + rejected != delivered) {
            throw new IllegalArgumentException(
                    "Accepted quantity plus rejected quantity must equal delivered quantity"
            );
        }
        if (delivered > orderItem.getRemainingQuantity()) {
            throw new IllegalArgumentException(
                    "Delivered quantity cannot exceed the remaining ordered quantity of "
                            + orderItem.getRemainingQuantity()
            );
        }
        if (rejected > 0 && (form.getRejectionReason() == null
                || form.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("Enter a reason for rejected products");
        }
        if (accepted > 0 && orderItem.getProduct().isBatchTracked()) {
            validateBatchInput(form.getBatchNumber(), form.getManufactureDate(),
                    form.getExpiryDate(), receivedDate);
        }
    }

    private void validateStoredItem(GoodsReceiptItem receiptItem,
                                    PurchaseOrderItem orderItem,
                                    LocalDate receivedDate) {
        GoodsReceiptItemForm form = new GoodsReceiptItemForm();
        form.setDeliveredQuantity(receiptItem.getDeliveredQuantity());
        form.setAcceptedQuantity(receiptItem.getAcceptedQuantity());
        form.setRejectedQuantity(receiptItem.getRejectedQuantity());
        form.setRejectionReason(receiptItem.getRejectionReason());
        form.setBatchNumber(receiptItem.getBatchNumber());
        form.setManufactureDate(receiptItem.getManufactureDate());
        form.setExpiryDate(receiptItem.getExpiryDate());
        validateItem(form, orderItem, receivedDate);
    }

    private void validateBatchInput(String batchNumber,
                                    LocalDate manufactureDate,
                                    LocalDate expiryDate,
                                    LocalDate receivedDate) {
        if (batchNumber == null || batchNumber.isBlank()) {
            throw new IllegalArgumentException("Batch number is required for batch-tracked products");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required for batch-tracked products");
        }
        if (manufactureDate != null && expiryDate.isBefore(manufactureDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before manufacture date");
        }
        if (receivedDate != null && expiryDate.isBefore(receivedDate)) {
            throw new IllegalArgumentException("Expired products cannot be accepted");
        }
    }

    private PurchaseOrder requireReceivableOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        if (!isReceivable(order)) {
            throw new IllegalArgumentException(
                    "Select an approved or partially received purchase order with remaining products"
            );
        }
        return order;
    }

    private StorageLocation requireActiveLocation(Long id) {
        StorageLocation location = storageLocationRepository.findByIdWithWarehouse(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage location not found"));
        if (!location.isActive() || !location.getWarehouse().isActive()) {
            throw new IllegalArgumentException("Select an active receiving location");
        }
        return location;
    }

    private boolean isReceivable(PurchaseOrder order) {
        boolean correctStatus = order.getStatus() == PurchaseOrderStatus.APPROVED
                || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED;
        return correctStatus && order.getItems().stream()
                .anyMatch(item -> item.getRemainingQuantity() > 0);
    }

    private void updatePurchaseOrderStatus(PurchaseOrder order) {
        boolean complete = order.getItems().stream()
                .allMatch(item -> item.getRemainingQuantity() == 0);
        boolean receivedSomething = order.getItems().stream()
                .anyMatch(item -> item.getReceivedQuantity() > 0);

        if (complete) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (receivedSomething) {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        } else {
            order.setStatus(PurchaseOrderStatus.APPROVED);
        }
    }

    private void applyForm(GoodsReceipt receipt,
                           GoodsReceiptForm form,
                           PurchaseOrder order,
                           StorageLocation location) {
        receipt.setPurchaseOrder(order);
        receipt.setLocation(location);
        receipt.setReceivedDate(form.getReceivedDate());
        receipt.setSupplierDeliveryReference(form.getSupplierDeliveryReference());
        receipt.setNotes(form.getNotes());
    }

    private void validateReceivedDate(LocalDate receivedDate, PurchaseOrder order) {
        if (receivedDate != null && order.getOrderDate() != null
                && receivedDate.isBefore(order.getOrderDate())) {
            throw new IllegalArgumentException("Received date cannot be before purchase-order date");
        }
        if (receivedDate != null && receivedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Received date cannot be in the future");
        }
    }

    private void rejectDuplicateDraft(Long purchaseOrderId, Long currentReceiptId) {
        boolean duplicate = currentReceiptId == null
                ? goodsReceiptRepository.existsByPurchaseOrderIdAndStatus(
                purchaseOrderId, GoodsReceiptStatus.DRAFT)
                : goodsReceiptRepository.existsByPurchaseOrderIdAndStatusAndIdNot(
                purchaseOrderId, GoodsReceiptStatus.DRAFT, currentReceiptId);
        if (duplicate) {
            throw new IllegalArgumentException(
                    "This purchase order already has an unfinished draft goods receipt"
            );
        }
    }

    private void requireDraft(GoodsReceipt receipt) {
        if (!receipt.isEditable()) {
            throw new IllegalArgumentException("Only draft goods receipts can be changed");
        }
    }

    private String generateReceiptNumber() {
        long nextNumber = goodsReceiptRepository.findTopByOrderByIdDesc()
                .map(receipt -> receipt.getId() + 1)
                .orElse(1L);
        return "GRN-" + LocalDate.now().getYear() + "-" + String.format("%04d", nextNumber);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
