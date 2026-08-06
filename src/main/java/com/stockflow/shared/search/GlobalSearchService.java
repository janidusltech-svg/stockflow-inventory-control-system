package com.stockflow.shared.search;

import com.stockflow.inventory.StockMovementRepository;
import com.stockflow.issuing.RecipientRepository;
import com.stockflow.issuing.StockIssueRepository;
import com.stockflow.issuing.StockReturnRepository;
import com.stockflow.product.ProductRepository;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.SupplierRepository;
import com.stockflow.receiving.GoodsReceiptRepository;
import com.stockflow.security.UserAccountRepository;
import com.stockflow.security.UserRole;
import com.stockflow.transfer.StockTransferRepository;
import com.stockflow.warehouse.StorageLocationRepository;
import com.stockflow.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class GlobalSearchService {

    private static final int PER_CATEGORY_LIMIT = 6;
    private static final int TOTAL_LIMIT = 42;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final StockTransferRepository stockTransferRepository;
    private final RecipientRepository recipientRepository;
    private final StockIssueRepository stockIssueRepository;
    private final StockReturnRepository stockReturnRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserAccountRepository userAccountRepository;

    public GlobalSearchService(ProductRepository productRepository,
                               SupplierRepository supplierRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               GoodsReceiptRepository goodsReceiptRepository,
                               WarehouseRepository warehouseRepository,
                               StorageLocationRepository storageLocationRepository,
                               StockTransferRepository stockTransferRepository,
                               RecipientRepository recipientRepository,
                               StockIssueRepository stockIssueRepository,
                               StockReturnRepository stockReturnRepository,
                               StockMovementRepository stockMovementRepository,
                               UserAccountRepository userAccountRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.warehouseRepository = warehouseRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.recipientRepository = recipientRepository;
        this.stockIssueRepository = stockIssueRepository;
        this.stockReturnRepository = stockReturnRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<GlobalSearchResult> search(String rawQuery, UserRole role) {
        String query = normalize(rawQuery);
        if (query.length() < 2) {
            return List.of();
        }
        List<GlobalSearchResult> results = new ArrayList<>();

        if (canViewProducts(role)) {
            productRepository.search(query).stream().limit(PER_CATEGORY_LIMIT).forEach(product ->
                    add(results, new GlobalSearchResult("Product",
                            product.getSku() + " - " + product.getName(),
                            product.getCategory().getName() + " · " + product.getUnit().getCode(),
                            product.isActive() ? "Active" : "Inactive",
                            "/products?keyword=" + encoded(product.getSku()))));
        }

        if (canViewPurchasing(role)) {
            supplierRepository.findBySupplierCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            query, query, query).stream().limit(PER_CATEGORY_LIMIT).forEach(supplier ->
                    add(results, new GlobalSearchResult("Supplier",
                            supplier.getSupplierCode() + " - " + supplier.getName(),
                            supplier.getEmail() + " · " + supplier.getPhone(),
                            supplier.isActive() ? "Active" : "Inactive",
                            "/suppliers?keyword=" + encoded(supplier.getSupplierCode()))));

            purchaseOrderRepository.findAllWithDetails().stream()
                    .filter(order -> contains(order.getOrderNumber(), query)
                            || contains(order.getSupplier().getName(), query)
                            || contains(order.getSupplier().getSupplierCode(), query)
                            || contains(order.getStatus().name(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(order -> add(results, new GlobalSearchResult("Purchase Order",
                            order.getOrderNumber(),
                            order.getSupplier().getName() + " · " + order.getOrderDate(),
                            readable(order.getStatus().name()),
                            "/purchase-orders/" + order.getId())));
        }

        if (canViewReceiving(role)) {
            goodsReceiptRepository.findAllWithDetails().stream()
                    .filter(receipt -> contains(receipt.getReceiptNumber(), query)
                            || contains(receipt.getPurchaseOrder().getOrderNumber(), query)
                            || contains(receipt.getPurchaseOrder().getSupplier().getName(), query)
                            || contains(receipt.getSupplierDeliveryReference(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(receipt -> add(results, new GlobalSearchResult("Goods Receipt",
                            receipt.getReceiptNumber(),
                            receipt.getPurchaseOrder().getOrderNumber() + " · "
                                    + receipt.getLocation().getDisplayName(),
                            readable(receipt.getStatus().name()),
                            "/goods-receipts/" + receipt.getId())));
        }

        if (canManageWarehouses(role)) {
            warehouseRepository.findAllByOrderByNameAsc().stream()
                    .filter(warehouse -> contains(warehouse.getCode(), query)
                            || contains(warehouse.getName(), query)
                            || contains(warehouse.getAddress(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(warehouse -> add(results, new GlobalSearchResult("Warehouse",
                            warehouse.getDisplayName(), value(warehouse.getAddress(), "No address recorded"),
                            warehouse.isActive() ? "Active" : "Inactive", "/warehouses")));

            storageLocationRepository.findAllWithWarehouse().stream()
                    .filter(location -> contains(location.getCode(), query)
                            || contains(location.getName(), query)
                            || contains(location.getWarehouse().getName(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(location -> add(results, new GlobalSearchResult("Storage Location",
                            location.getCode() + " - " + location.getName(),
                            location.getWarehouse().getName(),
                            location.isActive() ? "Active" : "Inactive", "/warehouses")));

            stockTransferRepository.findAllWithDetails().stream()
                    .filter(transfer -> contains(transfer.getTransferNumber(), query)
                            || contains(transfer.getSourceLocation().getDisplayName(), query)
                            || contains(transfer.getDestinationLocation().getDisplayName(), query)
                            || contains(transfer.getStatus().name(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(transfer -> add(results, new GlobalSearchResult("Stock Transfer",
                            transfer.getTransferNumber(),
                            transfer.getSourceLocation().getCode() + " → "
                                    + transfer.getDestinationLocation().getCode(),
                            readable(transfer.getStatus().name()), "/stock-transfers/" + transfer.getId())));
        }

        if (canViewIssues(role)) {
            recipientRepository.findAll().stream()
                    .filter(recipient -> contains(recipient.getCode(), query)
                            || contains(recipient.getName(), query)
                            || contains(recipient.getEmail(), query)
                            || contains(recipient.getPhone(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(recipient -> add(results, new GlobalSearchResult("Recipient",
                            recipient.getCode() + " - " + recipient.getName(),
                            readable(recipient.getRecipientType().name()),
                            recipient.isActive() ? "Active" : "Inactive",
                            "/recipients?keyword=" + encoded(recipient.getCode()))));

            stockIssueRepository.findAllWithDetails().stream()
                    .filter(issue -> contains(issue.getIssueNumber(), query)
                            || contains(issue.getRecipient().getName(), query)
                            || contains(issue.getExternalReference(), query)
                            || contains(issue.getStatus().name(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(issue -> add(results, new GlobalSearchResult("Stock Issue",
                            issue.getIssueNumber(), issue.getRecipient().getName() + " · " + issue.getIssueDate(),
                            readable(issue.getStatus().name()), "/stock-issues/" + issue.getId())));

            stockReturnRepository.findAllWithDetails().stream()
                    .filter(returned -> contains(returned.getReturnNumber(), query)
                            || contains(returned.getStockIssue().getIssueNumber(), query)
                            || contains(returned.getStockIssue().getRecipient().getName(), query)
                            || contains(returned.getStatus().name(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(returned -> add(results, new GlobalSearchResult("Stock Return",
                            returned.getReturnNumber(),
                            returned.getStockIssue().getIssueNumber() + " · " + returned.getReturnDate(),
                            readable(returned.getStatus().name()), "/stock-returns/" + returned.getId())));
        }

        if (canViewStock(role)) {
            stockMovementRepository.findAllWithDetailsNewestFirst().stream()
                    .filter(movement -> contains(movement.getReferenceNumber(), query)
                            || contains(movement.getProduct().getSku(), query)
                            || contains(movement.getProduct().getName(), query)
                            || contains(movement.getMovementType().name(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(movement -> add(results, new GlobalSearchResult("Stock Movement",
                            movement.getReferenceNumber(),
                            movement.getProduct().getSku() + " · " + movement.getLocation().getDisplayName(),
                            readable(movement.getMovementType().name()),
                            "/inventory-control/movements?keyword=" + encoded(movement.getReferenceNumber()))));
        }

        if (role == UserRole.IT_SYSTEMS_OFFICER) {
            userAccountRepository.findAll().stream()
                    .filter(account -> contains(account.getUsername(), query)
                            || contains(account.getFullName(), query)
                            || contains(account.getEmail(), query)
                            || contains(account.getRole().getDisplayName(), query))
                    .limit(PER_CATEGORY_LIMIT)
                    .forEach(account -> add(results, new GlobalSearchResult("User Account",
                            account.getFullName(), account.getUsername() + " · " + account.getEmail(),
                            account.isActive() ? "Active" : "Inactive",
                            "/user-accounts/" + account.getId() + "/edit")));
        }

        return results.stream().limit(TOTAL_LIMIT).toList();
    }

    private void add(List<GlobalSearchResult> results, GlobalSearchResult result) {
        if (results.size() < TOTAL_LIMIT) {
            results.add(result);
        }
    }

    private boolean canViewProducts(UserRole role) {
        return role != UserRole.IT_SYSTEMS_OFFICER;
    }

    private boolean canViewPurchasing(UserRole role) {
        return role == UserRole.OPERATIONS_MANAGER || role == UserRole.STORE_SUPERVISOR
                || role == UserRole.FINANCE_EXECUTIVE;
    }

    private boolean canViewReceiving(UserRole role) {
        return canViewPurchasing(role);
    }

    private boolean canManageWarehouses(UserRole role) {
        return role == UserRole.OPERATIONS_MANAGER || role == UserRole.STORE_SUPERVISOR;
    }

    private boolean canViewIssues(UserRole role) {
        return role == UserRole.OPERATIONS_MANAGER || role == UserRole.STORE_SUPERVISOR
                || role == UserRole.CUSTOMER_SERVICE_COORDINATOR;
    }

    private boolean canViewStock(UserRole role) {
        return role != UserRole.IT_SYSTEMS_OFFICER;
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String readable(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
