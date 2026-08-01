package com.stockflow.inventory;

import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryBalanceRepository
            inventoryBalanceRepository;

    private final StockMovementRepository
            stockMovementRepository;

    public InventoryService(
            InventoryBalanceRepository inventoryBalanceRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.inventoryBalanceRepository =
                inventoryBalanceRepository;

        this.stockMovementRepository =
                stockMovementRepository;
    }

    public void receive(
            Product product,
            StorageLocation location,
            Batch batch,
            int acceptedQuantity,
            String receiptNumber
    ) {
        if (acceptedQuantity <= 0) {
            return;
        }

        InventoryBalance balance;

        if (batch == null) {
            balance = inventoryBalanceRepository
                    .findByProductAndLocationAndBatchIsNull(
                            product,
                            location
                    )
                    .orElseGet(() ->
                            newBalance(
                                    product,
                                    location,
                                    null
                            )
                    );
        } else {
            balance = inventoryBalanceRepository
                    .findByProductAndLocationAndBatch(
                            product,
                            location,
                            batch
                    )
                    .orElseGet(() ->
                            newBalance(
                                    product,
                                    location,
                                    batch
                            )
                    );
        }

        balance.addQuantity(acceptedQuantity);
        inventoryBalanceRepository.save(balance);

        StockMovement movement =
                new StockMovement();

        movement.setMovementType(
                StockMovementType.GOODS_RECEIPT
        );
        movement.setProduct(product);
        movement.setLocation(location);
        movement.setBatch(batch);
        movement.setQuantityChange(acceptedQuantity);
        movement.setReferenceNumber(receiptNumber);

        stockMovementRepository.save(movement);
    }

    private InventoryBalance newBalance(
            Product product,
            StorageLocation location,
            Batch batch
    ) {
        InventoryBalance balance =
                new InventoryBalance();

        balance.setProduct(product);
        balance.setLocation(location);
        balance.setBatch(batch);
        balance.setQuantityAvailable(0);

        return balance;
    }
}