package com.stockflow.control;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    long countByStatus(AdjustmentStatus status);

    long countByStockCountIdAndStatus(Long stockCountId, AdjustmentStatus status);

    @Query("""
            select adjustment
            from StockAdjustment adjustment
            join fetch adjustment.inventoryBalance balance
            join fetch balance.product product
            join fetch product.unit
            join fetch balance.location location
            join fetch location.warehouse
            left join fetch balance.batch
            left join fetch adjustment.stockCount
            order by adjustment.createdAt desc
            """)
    List<StockAdjustment> findAllWithDetails();

    @Query("""
            select adjustment
            from StockAdjustment adjustment
            join fetch adjustment.inventoryBalance balance
            join fetch balance.product product
            join fetch product.unit
            join fetch balance.location location
            join fetch location.warehouse
            left join fetch balance.batch
            left join fetch adjustment.stockCount
            where adjustment.id = :id
            """)
    Optional<StockAdjustment> findByIdWithDetails(@Param("id") Long id);
}
