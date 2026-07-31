package com.stockflow.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
            select distinct purchaseOrder
            from PurchaseOrder purchaseOrder
            join fetch purchaseOrder.supplier
            left join fetch purchaseOrder.items item
            left join fetch item.product product
            left join fetch product.unit
            order by purchaseOrder.orderDate desc, purchaseOrder.id desc
            """)
    List<PurchaseOrder> findAllWithDetails();

    @Query("""
            select distinct purchaseOrder
            from PurchaseOrder purchaseOrder
            join fetch purchaseOrder.supplier
            left join fetch purchaseOrder.items item
            left join fetch item.product product
            left join fetch product.unit
            where purchaseOrder.id = :id
            """)
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    Optional<PurchaseOrder> findTopByOrderByIdDesc();
}
