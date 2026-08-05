package com.stockflow.inventory;

import com.stockflow.product.Product;
import com.stockflow.warehouse.StorageLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(InventoryBalanceRepository inventoryBalanceRepository,
                            StockMovementRepository stockMovementRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public void receive(Product product,
                        StorageLocation location,
                        Batch batch,
                        int acceptedQuantity,
                        String receiptNumber) {
        if (acceptedQuantity <= 0) {
            return;
        }

        InventoryBalance balance = findOrCreateBalance(product, location, batch);
        balance.addQuantity(acceptedQuantity);
        inventoryBalanceRepository.save(balance);
        saveMovement(
                StockMovementType.GOODS_RECEIPT,
                product,
                location,
                batch,
                acceptedQuantity,
                receiptNumber
        );
    }

    public void transferOut(Product product,
                            StorageLocation sourceLocation,
                            Batch batch,
                            int quantity,
                            String transferNumber) {
        InventoryBalance sourceBalance = findExistingBalance(
                product,
                sourceLocation,
                batch
        );
        sourceBalance.removeQuantity(quantity);
        inventoryBalanceRepository.save(sourceBalance);
        saveMovement(
                StockMovementType.TRANSFER_OUT,
                product,
                sourceLocation,
                batch,
                -quantity,
                transferNumber
        );
    }

    public void transferIn(Product product,
                           StorageLocation destinationLocation,
                           Batch batch,
                           int quantity,
                           String transferNumber) {
        InventoryBalance destinationBalance = findOrCreateBalance(
                product,
                destinationLocation,
                batch
        );
        destinationBalance.addQuantity(quantity);
        inventoryBalanceRepository.save(destinationBalance);
        saveMovement(
                StockMovementType.TRANSFER_IN,
                product,
                destinationLocation,
                batch,
                quantity,
                transferNumber
        );
    }

    public void issue(Product product,
                      StorageLocation location,
                      Batch batch,
                      int quantity,
                      String issueNumber) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Issue quantity must be greater than zero");
        }

        InventoryBalance balance = findExistingBalance(product, location, batch);
        balance.removeQuantity(quantity);
        inventoryBalanceRepository.save(balance);
        saveMovement(
                StockMovementType.STOCK_ISSUE,
                product,
                location,
                batch,
                -quantity,
                issueNumber
        );
    }

    public void returnToStock(Product product,
                              StorageLocation location,
                              Batch batch,
                              int quantity,
                              String returnNumber) {
        if (quantity <= 0) {
            return;
        }

        InventoryBalance balance = findOrCreateBalance(product, location, batch);
        balance.addQuantity(quantity);
        inventoryBalanceRepository.save(balance);
        saveMovement(
                StockMovementType.STOCK_RETURN,
                product,
                location,
                batch,
                quantity,
                returnNumber
        );
    }

    private InventoryBalance findExistingBalance(Product product,
                                                 StorageLocation location,
                                                 Batch batch) {
        return batch == null
                ? inventoryBalanceRepository
                .findByProductAndLocationAndBatchIsNull(product, location)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No stock balance exists at the source location"
                ))
                : inventoryBalanceRepository
                .findByProductAndLocationAndBatch(product, location, batch)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No batch stock exists at the source location"
                ));
    }

    private InventoryBalance findOrCreateBalance(Product product,
                                                 StorageLocation location,
                                                 Batch batch) {
        return batch == null
                ? inventoryBalanceRepository
                .findByProductAndLocationAndBatchIsNull(product, location)
                .orElseGet(() -> newBalance(product, location, null))
                : inventoryBalanceRepository
                .findByProductAndLocationAndBatch(product, location, batch)
                .orElseGet(() -> newBalance(product, location, batch));
    }

    private InventoryBalance newBalance(Product product,
                                        StorageLocation location,
                                        Batch batch) {
        InventoryBalance balance = new InventoryBalance();
        balance.setProduct(product);
        balance.setLocation(location);
        balance.setBatch(batch);
        balance.setQuantityAvailable(0);
        return balance;
    }

    private void saveMovement(StockMovementType movementType,
                              Product product,
                              StorageLocation location,
                              Batch batch,
                              int quantityChange,
                              String referenceNumber) {
        StockMovement movement = new StockMovement();
        movement.setMovementType(movementType);
        movement.setProduct(product);
        movement.setLocation(location);
        movement.setBatch(batch);
        movement.setQuantityChange(quantityChange);
        movement.setReferenceNumber(referenceNumber);
        stockMovementRepository.save(movement);
    }
}
