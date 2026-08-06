package com.stockflow.shared.notification;

import com.stockflow.control.AdjustmentStatus;
import com.stockflow.control.InventoryControlService;
import com.stockflow.control.ReorderAlert;
import com.stockflow.control.StockAdjustmentRepository;
import com.stockflow.control.StockCountRepository;
import com.stockflow.control.StockCountStatus;
import com.stockflow.inventory.InventoryBalance;
import com.stockflow.issuing.StockIssueRepository;
import com.stockflow.issuing.StockIssueStatus;
import com.stockflow.purchasing.PurchaseOrder;
import com.stockflow.purchasing.PurchaseOrderRepository;
import com.stockflow.purchasing.PurchaseOrderStatus;
import com.stockflow.receiving.GoodsReceiptRepository;
import com.stockflow.receiving.GoodsReceiptStatus;
import com.stockflow.security.UserAccountRepository;
import com.stockflow.security.UserRole;
import com.stockflow.transfer.StockTransfer;
import com.stockflow.transfer.StockTransferRepository;
import com.stockflow.transfer.StockTransferStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class NotificationService {

    private static final String AUTO_PREFIX = "AUTO:";

    private final SystemNotificationRepository notificationRepository;
    private final InventoryControlService inventoryControlService;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final StockCountRepository stockCountRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final StockTransferRepository stockTransferRepository;
    private final StockIssueRepository stockIssueRepository;
    private final UserAccountRepository userAccountRepository;

    public NotificationService(SystemNotificationRepository notificationRepository,
                               InventoryControlService inventoryControlService,
                               StockAdjustmentRepository stockAdjustmentRepository,
                               StockCountRepository stockCountRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               GoodsReceiptRepository goodsReceiptRepository,
                               StockTransferRepository stockTransferRepository,
                               StockIssueRepository stockIssueRepository,
                               UserAccountRepository userAccountRepository) {
        this.notificationRepository = notificationRepository;
        this.inventoryControlService = inventoryControlService;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.stockCountRepository = stockCountRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.stockIssueRepository = stockIssueRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<SystemNotification> refreshAndFindAll(String username, UserRole role) {
        refresh(username, role);
        return notificationRepository.findByUsernameOrderByReadFlagAscUpdatedAtDesc(username);
    }

    public long refreshAndCountUnread(String username, UserRole role) {
        refresh(username, role);
        return notificationRepository.countByUsernameAndReadFlagFalse(username);
    }

    public void markRead(Long id, String username) {
        SystemNotification notification = notificationRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new IllegalArgumentException("Notification was not found"));
        notification.setReadFlag(true);
        notificationRepository.save(notification);
    }

    public void markAllRead(String username) {
        List<SystemNotification> notifications = notificationRepository.findByUsername(username);
        notifications.forEach(notification -> notification.setReadFlag(true));
        notificationRepository.saveAll(notifications);
    }

    private void refresh(String username, UserRole role) {
        List<NotificationSpec> specs = buildSpecs(role);
        List<SystemNotification> existing = notificationRepository.findByUsername(username);
        Map<String, SystemNotification> bySource = new HashMap<>();
        existing.forEach(notification -> bySource.put(notification.getSourceKey(), notification));

        Set<String> activeKeys = new HashSet<>();
        List<SystemNotification> toSave = new ArrayList<>();
        for (NotificationSpec spec : specs) {
            activeKeys.add(spec.sourceKey());
            SystemNotification notification = bySource.get(spec.sourceKey());
            if (notification == null) {
                notification = new SystemNotification(username, spec.sourceKey(), spec.type(),
                        spec.title(), spec.message(), spec.targetUrl());
            } else {
                notification.updateContent(spec.type(), spec.title(), spec.message(), spec.targetUrl());
            }
            toSave.add(notification);
        }
        notificationRepository.saveAll(toSave);

        List<SystemNotification> stale = existing.stream()
                .filter(notification -> notification.getSourceKey().startsWith(AUTO_PREFIX))
                .filter(notification -> !activeKeys.contains(notification.getSourceKey()))
                .toList();
        notificationRepository.deleteAll(stale);
    }

    private List<NotificationSpec> buildSpecs(UserRole role) {
        List<NotificationSpec> specs = new ArrayList<>();
        if (role == UserRole.OPERATIONS_MANAGER) {
            addOperationsNotifications(specs);
        } else if (role == UserRole.STORE_SUPERVISOR) {
            addStoreNotifications(specs);
        } else if (role == UserRole.FINANCE_EXECUTIVE) {
            addFinanceNotifications(specs);
        } else if (role == UserRole.CUSTOMER_SERVICE_COORDINATOR) {
            addCustomerServiceNotifications(specs);
        } else if (role == UserRole.IT_SYSTEMS_OFFICER) {
            addItNotifications(specs);
        }
        return specs;
    }

    private void addOperationsNotifications(List<NotificationSpec> specs) {
        for (ReorderAlert alert : inventoryControlService.findPriorityAlerts()) {
            NotificationType type = alert.isOutOfStock()
                    ? NotificationType.CRITICAL : NotificationType.WARNING;
            String title = alert.isOutOfStock() ? "Product is out of stock" : "Product reached its reorder level";
            specs.add(spec(AUTO_PREFIX + "REORDER:" + alert.rule().getProduct().getId(), type,
                    title,
                    alert.rule().getProduct().getSku() + " - " + alert.rule().getProduct().getName()
                            + " has " + alert.currentQuantity() + " units available.",
                    "/reorder-rules"));
        }
        for (InventoryBalance balance : inventoryControlService.findExpiringBatches().stream().limit(10).toList()) {
            specs.add(spec(AUTO_PREFIX + "EXPIRY:" + balance.getId(), NotificationType.WARNING,
                    "Batch is approaching expiry",
                    balance.getProduct().getSku() + " batch " + balance.getBatch().getBatchNumber()
                            + " expires on " + balance.getBatch().getExpiryDate() + ".",
                    "/inventory-control#expiry"));
        }
        addCount(specs, AUTO_PREFIX + "PENDING_ADJUSTMENTS",
                stockAdjustmentRepository.countByStatus(AdjustmentStatus.PENDING),
                NotificationType.WARNING, "Stock adjustments require review",
                "pending stock adjustment request(s) are waiting for a decision.", "/stock-adjustments?status=PENDING");
        addCount(specs, AUTO_PREFIX + "SUBMITTED_COUNTS",
                stockCountRepository.countByStatus(StockCountStatus.SUBMITTED),
                NotificationType.WARNING, "Submitted stock counts require review",
                "submitted stock count(s) are ready to be checked and closed.", "/stock-counts");
        long draftTransfers = stockTransferRepository.findAllWithDetails().stream()
                .filter(transfer -> transfer.getStatus() == StockTransferStatus.DRAFT).count();
        addCount(specs, AUTO_PREFIX + "DRAFT_TRANSFERS", draftTransfers,
                NotificationType.INFORMATION, "Stock transfers are waiting for approval",
                "draft stock transfer(s) can be reviewed.", "/stock-transfers");
    }

    private void addStoreNotifications(List<NotificationSpec> specs) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllWithDetails();
        long readyToReceive = orders.stream().filter(order -> order.getStatus() == PurchaseOrderStatus.APPROVED
                || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED).count();
        addCount(specs, AUTO_PREFIX + "READY_TO_RECEIVE", readyToReceive,
                NotificationType.INFORMATION, "Purchase orders are ready for receiving",
                "approved or partially received purchase order(s) can receive deliveries.", "/goods-receipts/new");

        List<StockTransfer> transfers = stockTransferRepository.findAllWithDetails();
        long readyToDispatch = transfers.stream().filter(t -> t.getStatus() == StockTransferStatus.APPROVED).count();
        long readyToAccept = transfers.stream().filter(t -> t.getStatus() == StockTransferStatus.DISPATCHED).count();
        addCount(specs, AUTO_PREFIX + "READY_TO_DISPATCH", readyToDispatch,
                NotificationType.INFORMATION, "Approved transfers are ready for dispatch",
                "approved stock transfer(s) are waiting to be dispatched.", "/stock-transfers");
        addCount(specs, AUTO_PREFIX + "READY_TO_ACCEPT", readyToAccept,
                NotificationType.WARNING, "Dispatched transfers are waiting for receipt",
                "dispatched stock transfer(s) must be received at their destination.", "/stock-transfers");

        long draftReceipts = goodsReceiptRepository.findAllWithDetails().stream()
                .filter(receipt -> receipt.getStatus() == GoodsReceiptStatus.DRAFT).count();
        addCount(specs, AUTO_PREFIX + "DRAFT_RECEIPTS", draftReceipts,
                NotificationType.INFORMATION, "Goods receipts remain in draft",
                "draft goods receipt(s) need to be completed or cancelled.", "/goods-receipts");
    }

    private void addFinanceNotifications(List<NotificationSpec> specs) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllWithDetails();
        long drafts = orders.stream().filter(order -> order.getStatus() == PurchaseOrderStatus.DRAFT).count();
        long partial = orders.stream().filter(order -> order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED).count();
        addCount(specs, AUTO_PREFIX + "DRAFT_PURCHASE_ORDERS", drafts,
                NotificationType.INFORMATION, "Purchase orders remain in draft",
                "draft purchase order(s) can be reviewed and approved.", "/purchase-orders");
        addCount(specs, AUTO_PREFIX + "PARTIAL_PURCHASE_ORDERS", partial,
                NotificationType.WARNING, "Purchase orders are partially received",
                "purchase order(s) still have quantities awaiting delivery.", "/purchase-orders");
    }

    private void addCustomerServiceNotifications(List<NotificationSpec> specs) {
        long returnableIssues = stockIssueRepository.findAllWithDetails().stream()
                .filter(issue -> issue.getStatus() == StockIssueStatus.CONFIRMED
                        || issue.getStatus() == StockIssueStatus.PARTIALLY_RETURNED)
                .count();
        addCount(specs, AUTO_PREFIX + "RETURNABLE_ISSUES", returnableIssues,
                NotificationType.INFORMATION, "Issued orders are available for follow-up",
                "confirmed or partially returned issue record(s) can be checked for customers.", "/stock-issues");
    }

    private void addItNotifications(List<NotificationSpec> specs) {
        long inactiveUsers = userAccountRepository.findAll().stream().filter(account -> !account.isActive()).count();
        addCount(specs, AUTO_PREFIX + "INACTIVE_USERS", inactiveUsers,
                NotificationType.INFORMATION, "Inactive user accounts are recorded",
                "inactive account(s) are currently prevented from signing in.", "/user-accounts");
    }

    private void addCount(List<NotificationSpec> specs, String key, long count,
                          NotificationType type, String title, String messageSuffix, String targetUrl) {
        if (count > 0) {
            specs.add(spec(key, type, title, count + " " + messageSuffix, targetUrl));
        }
    }

    private NotificationSpec spec(String sourceKey, NotificationType type,
                                  String title, String message, String targetUrl) {
        return new NotificationSpec(sourceKey, type, title, message, targetUrl);
    }

    private record NotificationSpec(String sourceKey, NotificationType type,
                                    String title, String message, String targetUrl) {
    }
}
