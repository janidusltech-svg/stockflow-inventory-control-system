package com.stockflow.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse warehouse
            order by warehouse.name, location.name
            """)
    List<StorageLocation> findAllWithWarehouse();

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse warehouse
            where location.active = true and warehouse.active = true
            order by warehouse.name, location.name
            """)
    List<StorageLocation> findAllActiveWithWarehouse();

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse
            where location.id = :id
            """)
    Optional<StorageLocation> findByIdWithWarehouse(@Param("id") Long id);

    @Query("""
            select location
            from StorageLocation location
            join fetch location.warehouse
            where location.warehouse.id = :warehouseId
            order by location.name
            """)
    List<StorageLocation> findByWarehouseIdWithWarehouse(
            @Param("warehouseId") Long warehouseId
    );

    long countByWarehouseId(Long warehouseId);

    @Query("select distinct location.warehouse.id from StorageLocation location")
    List<Long> findWarehouseIdsWithLocations();

    // Safe-delete checks: each query lists the location ids already used by
    // another StockFlow record. A location found in any list must be kept.

    @Query("select distinct balance.location.id from InventoryBalance balance")
    List<Long> findLocationIdsUsedInInventoryBalances();

    @Query("select distinct movement.location.id from StockMovement movement")
    List<Long> findLocationIdsUsedInStockMovements();

    @Query("select distinct receipt.location.id from GoodsReceipt receipt")
    List<Long> findLocationIdsUsedInGoodsReceipts();

    @Query("select distinct transfer.sourceLocation.id from StockTransfer transfer")
    List<Long> findLocationIdsUsedAsTransferSource();

    @Query("select distinct transfer.destinationLocation.id from StockTransfer transfer")
    List<Long> findLocationIdsUsedAsTransferDestination();

    @Query("select distinct item.location.id from StockIssueItem item")
    List<Long> findLocationIdsUsedInStockIssues();

    @Query("select distinct stockCount.location.id from StockCount stockCount")
    List<Long> findLocationIdsUsedInStockCounts();

    boolean existsByWarehouseIdAndCodeIgnoreCase(Long warehouseId, String code);

    boolean existsByWarehouseIdAndCodeIgnoreCaseAndIdNot(
            Long warehouseId,
            String code,
            Long id
    );
}
