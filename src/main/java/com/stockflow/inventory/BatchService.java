package com.stockflow.inventory;

import com.stockflow.product.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class BatchService {

    private final BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public Batch resolveBatch(Product product,
                              String batchNumber,
                              LocalDate manufactureDate,
                              LocalDate expiryDate,
                              LocalDate receivedDate) {
        if (!product.isBatchTracked()) {
            return null;
        }

        String cleanedBatchNumber = batchNumber == null ? "" : batchNumber.trim().toUpperCase();
        if (cleanedBatchNumber.isBlank()) {
            throw new IllegalArgumentException("Batch number is required for " + product.getName());
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required for " + product.getName());
        }
        if (manufactureDate != null && expiryDate.isBefore(manufactureDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before manufacture date");
        }
        if (receivedDate != null && expiryDate.isBefore(receivedDate)) {
            throw new IllegalArgumentException("An expired batch cannot be accepted into inventory");
        }

        return batchRepository
                .findByProductIdAndBatchNumberIgnoreCase(product.getId(), cleanedBatchNumber)
                .map(existing -> validateExisting(existing, manufactureDate, expiryDate))
                .orElseGet(() -> create(product, cleanedBatchNumber, manufactureDate, expiryDate));
    }

    private Batch validateExisting(Batch batch,
                                   LocalDate manufactureDate,
                                   LocalDate expiryDate) {
        if (batch.getManufactureDate() != null
                && manufactureDate != null
                && !batch.getManufactureDate().equals(manufactureDate)) {
            throw new IllegalArgumentException("This batch number already has a different manufacture date");
        }
        if (batch.getExpiryDate() != null && !batch.getExpiryDate().equals(expiryDate)) {
            throw new IllegalArgumentException("This batch number already has a different expiry date");
        }
        if (batch.getStatus() == BatchStatus.EXPIRED) {
            throw new IllegalArgumentException("This batch is already marked as expired");
        }
        return batch;
    }

    private Batch create(Product product,
                         String batchNumber,
                         LocalDate manufactureDate,
                         LocalDate expiryDate) {
        Batch batch = new Batch();
        batch.setProduct(product);
        batch.setBatchNumber(batchNumber);
        batch.setManufactureDate(manufactureDate);
        batch.setExpiryDate(expiryDate);
        return batchRepository.save(batch);
    }
}
