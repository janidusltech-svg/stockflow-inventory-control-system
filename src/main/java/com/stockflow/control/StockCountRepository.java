package com.stockflow.control;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    long countByStatus(StockCountStatus status);

    boolean existsByCountNumber(String countNumber);

    @Query("""
            select distinct stockCount
            from StockCount stockCount
            join fetch stockCount.location location
            join fetch location.warehouse
            left join fetch stockCount.items
            order by stockCount.createdAt desc
            """)
    List<StockCount> findAllWithLocationAndItems();

    @Query("""
            select distinct stockCount
            from StockCount stockCount
            join fetch stockCount.location location
            join fetch location.warehouse
            left join fetch stockCount.items item
            left join fetch item.inventoryBalance balance
            left join fetch balance.product product
            left join fetch product.unit
            left join fetch balance.batch
            where stockCount.id = :id
            """)
    Optional<StockCount> findByIdWithDetails(@Param("id") Long id);
}
