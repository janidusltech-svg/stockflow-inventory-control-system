package com.stockflow.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("""
            select movement
            from StockMovement movement
            join fetch movement.product product
            join fetch product.unit
            join fetch movement.location location
            join fetch location.warehouse
            left join fetch movement.batch
            order by movement.createdAt desc
            """)
    List<StockMovement> findAllWithDetailsNewestFirst();
}
