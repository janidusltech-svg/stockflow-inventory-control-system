package com.stockflow.purchasing;

import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final ProductRepository productRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierService supplierService,
            ProductRepository productRepository
    ) {
        this.purchaseOrderRepository =
                purchaseOrderRepository;
        this.supplierService = supplierService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll(
            String keyword,
            PurchaseOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        String cleanedKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return purchaseOrderRepository
                .findAllWithDetails()
                .stream()
                .filter(order ->
                        cleanedKeyword.isBlank()
                                || order.getOrderNumber()
                                .toLowerCase(Locale.ROOT)
                                .contains(cleanedKeyword)
                                || order.getSupplier()
                                .getName()
                                .toLowerCase(Locale.ROOT)
                                .contains(cleanedKeyword)
                )
                .filter(order ->
                        status == null
                                || order.getStatus() == status
                )
                .filter(order ->
                        fromDate == null
                                || !order.getOrderDate()
                                .isBefore(fromDate)
                )
                .filter(order ->
                        toDate == null
                                || !order.getOrderDate()
                                .isAfter(toDate)
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository
                .findByIdWithDetails(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Purchase order not found"
                        )
                );
    }

    public PurchaseOrder create(
            PurchaseOrderForm form
    ) {
        validateDates(form);

        Supplier supplier =
                supplierService.findById(
                        form.getSupplierId()
                );

        if (!supplier.isActive()) {
            throw new IllegalArgumentException(
                    "Select an active supplier"
            );
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setSupplier(supplier);
        order.setOrderDate(form.getOrderDate());
        order.setExpectedDate(form.getExpectedDate());
        order.setNotes(form.getNotes());

        return purchaseOrderRepository.save(order);
    }

    public PurchaseOrder update(
            Long id,
            PurchaseOrderForm form
    ) {
        validateDates(form);

        PurchaseOrder order = findById(id);
        requireDraft(order);

        Supplier supplier =
                supplierService.findById(
                        form.getSupplierId()
                );

        if (!supplier.isActive()
                && !supplier.getId()
                .equals(order.getSupplier().getId())) {

            throw new IllegalArgumentException(
                    "Select an active supplier"
            );
        }

        order.setSupplier(supplier);
        order.setOrderDate(form.getOrderDate());
        order.setExpectedDate(form.getExpectedDate());
        order.setNotes(form.getNotes());

        return purchaseOrderRepository.save(order);
    }

    public void addItem(
            Long orderId,
            PurchaseOrderItemForm form
    ) {
        PurchaseOrder order = findById(orderId);
        requireDraft(order);

        Product product = productRepository
                .findById(form.getProductId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "Select an active product"
            );
        }

        boolean duplicateProduct =
                order.getItems()
                        .stream()
                        .anyMatch(item ->
                                item.getProduct()
                                        .getId()
                                        .equals(product.getId())
                        );

        if (duplicateProduct) {
            throw new IllegalArgumentException(
                    "This product is already included in the purchase order"
            );
        }

        PurchaseOrderItem item =
                new PurchaseOrderItem();

        item.setPurchaseOrder(order);
        item.setProduct(product);
        item.setQuantity(form.getQuantity());
        item.setUnitPrice(form.getUnitPrice());

        order.getItems().add(item);
        purchaseOrderRepository.save(order);
    }

    public void removeItem(
            Long orderId,
            Long itemId
    ) {
        PurchaseOrder order = findById(orderId);
        requireDraft(order);

        boolean removed = order.getItems()
                .removeIf(item ->
                        item.getId().equals(itemId)
                );

        if (!removed) {
            throw new IllegalArgumentException(
                    "Purchase order item not found"
            );
        }

        purchaseOrderRepository.save(order);
    }

    public void changeStatus(
            Long orderId,
            PurchaseOrderStatus newStatus
    ) {
        PurchaseOrder order = findById(orderId);

        if (newStatus == PurchaseOrderStatus.APPROVED) {
            if (order.getStatus()
                    != PurchaseOrderStatus.DRAFT) {

                throw new IllegalArgumentException(
                        "Only draft purchase orders can be approved"
                );
            }

            if (order.getItems().isEmpty()) {
                throw new IllegalArgumentException(
                        "Add at least one item before approving the purchase order"
                );
            }
        } else if (
                newStatus == PurchaseOrderStatus.CANCELLED
        ) {
            if (order.getStatus()
                    == PurchaseOrderStatus.RECEIVED
                    || order.getStatus()
                    == PurchaseOrderStatus.PARTIALLY_RECEIVED
                    || order.getStatus()
                    == PurchaseOrderStatus.CANCELLED) {

                throw new IllegalArgumentException(
                        "This purchase order cannot be cancelled"
                );
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported purchase order status change"
            );
        }

        order.setStatus(newStatus);
        purchaseOrderRepository.save(order);
    }

    public PurchaseOrderForm toForm(
            PurchaseOrder order
    ) {
        PurchaseOrderForm form =
                new PurchaseOrderForm();

        form.setSupplierId(
                order.getSupplier().getId()
        );
        form.setOrderDate(order.getOrderDate());
        form.setExpectedDate(order.getExpectedDate());
        form.setNotes(order.getNotes());

        return form;
    }

    private void validateDates(
            PurchaseOrderForm form
    ) {
        if (form.getOrderDate() != null
                && form.getExpectedDate() != null
                && form.getExpectedDate()
                .isBefore(form.getOrderDate())) {

            throw new IllegalArgumentException(
                    "Expected date cannot be before the order date"
            );
        }
    }

    private void requireDraft(
            PurchaseOrder order
    ) {
        if (!order.isEditable()) {
            throw new IllegalArgumentException(
                    "Only draft purchase orders can be edited"
            );
        }
    }

    private String generateOrderNumber() {
        long nextNumber =
                purchaseOrderRepository
                        .findTopByOrderByIdDesc()
                        .map(order -> order.getId() + 1)
                        .orElse(1L);

        return "PO-"
                + LocalDate.now().getYear()
                + "-"
                + String.format("%04d", nextNumber);
    }
}