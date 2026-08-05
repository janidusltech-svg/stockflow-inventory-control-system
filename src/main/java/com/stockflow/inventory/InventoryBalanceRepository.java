package com.stockflow.inventory;

import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryBalance> findByProductAndLocationAndBatch(
            Product product,
            StorageLocation location,
            Batch batch
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
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
            join fetch location.warehouse warehouse
            left join fetch balance.batch batch
            where balance.quantityAvailable > 0
              and product.active = true
              and location.active = true
              and warehouse.active = true
            order by product.name, warehouse.name, location.name, batch.batchNumber
            """)
    List<InventoryBalance> findAllAvailableWithDetails();

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
            order by product.name, location.warehouse.name, location.name, batch.batchNumber
            """)
    List<InventoryBalance> findAllWithDetails();

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
            order by product.name, batch.batchNumber
            """)
    List<InventoryBalance> findAllByLocationIdWithDetails(
            @Param("locationId") Long locationId
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
            join fetch balance.batch batch
            where balance.quantityAvailable > 0
              and batch.expiryDate is not null
              and batch.expiryDate between :fromDate and :toDate
            order by batch.expiryDate, product.name
            """)
    List<InventoryBalance> findExpiringWithDetails(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("select coalesce(sum(balance.quantityAvailable), 0) from InventoryBalance balance")
    Long sumAvailableQuantity();

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
