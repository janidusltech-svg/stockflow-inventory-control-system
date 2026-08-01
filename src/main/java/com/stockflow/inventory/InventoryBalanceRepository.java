package com.stockflow.inventory;

import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryBalanceRepository
        extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance>
    findByProductAndLocationAndBatch(
            Product product,
            StorageLocation location,
            Batch batch
    );

    Optional<InventoryBalance>
    findByProductAndLocationAndBatchIsNull(
            Product product,
            StorageLocation location
    );
}