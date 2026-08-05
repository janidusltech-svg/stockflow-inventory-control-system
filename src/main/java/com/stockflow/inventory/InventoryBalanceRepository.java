package com.stockflow.inventory;

import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance> findByProductAndLocationAndBatch(
            Product product,
            StorageLocation location,
            Batch batch
    );

    Optional<InventoryBalance> findByProductAndLocationAndBatchIsNull(
            Product product,
            StorageLocation location
    );

    @Query("""
            select balance
            from InventoryBalance balance
            join fetch balance.product product
            join fetch product.category
            left join fetch product.brand
            join fetch product.unit
            join fetch balance.location location
            join fetch location.warehouse
            left join fetch balance.batch
            where balance.id = :id
            """)
    Optional<InventoryBalance> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            select balance
            from InventoryBalance balance
            join fetch balance.product product
            join fetch product.category
            left join fetch product.brand
            join fetch product.unit
            join fetch balance.location location
            join fetch location.warehouse
            left join fetch balance.batch batch
            where location.id = :locationId
              and balance.quantityAvailable > 0
            order by product.name, batch.expiryDate
            """)
    List<InventoryBalance> findAvailableByLocationId(
            @Param("locationId") Long locationId
    );

    @Query("""
            select count(balance)
            from InventoryBalance balance
            where balance.location.id = :locationId
              and balance.quantityAvailable > 0
            """)
    long countPositiveByLocationId(@Param("locationId") Long locationId);

    @Query("""
            select count(balance)
            from InventoryBalance balance
            where balance.location.warehouse.id = :warehouseId
              and balance.quantityAvailable > 0
            """)
    long countPositiveByWarehouseId(@Param("warehouseId") Long warehouseId);
}
