package com.stockflow.receiving;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query("""
            select distinct receipt
            from GoodsReceipt receipt
            join fetch receipt.purchaseOrder purchaseOrder
            join fetch purchaseOrder.supplier
            join fetch receipt.location location
            join fetch location.warehouse
            left join fetch receipt.items receiptItem
            left join fetch receiptItem.purchaseOrderItem
            left join fetch receiptItem.product product
            left join fetch product.unit
            left join fetch receiptItem.batch
            order by receipt.receivedDate desc, receipt.id desc
            """)
    List<GoodsReceipt> findAllWithDetails();

    @Query("""
            select distinct receipt
            from GoodsReceipt receipt
            join fetch receipt.purchaseOrder purchaseOrder
            join fetch purchaseOrder.supplier
            join fetch receipt.location location
            join fetch location.warehouse
            left join fetch receipt.items receiptItem
            left join fetch receiptItem.purchaseOrderItem
            left join fetch receiptItem.product product
            left join fetch product.unit
            left join fetch receiptItem.batch
            where receipt.id = :id
            """)
    Optional<GoodsReceipt> findByIdWithDetails(@Param("id") Long id);

    Optional<GoodsReceipt> findTopByOrderByIdDesc();

    boolean existsByPurchaseOrderIdAndStatus(Long purchaseOrderId, GoodsReceiptStatus status);

    boolean existsByPurchaseOrderIdAndStatusAndIdNot(
            Long purchaseOrderId,
            GoodsReceiptStatus status,
            Long id
    );
}
