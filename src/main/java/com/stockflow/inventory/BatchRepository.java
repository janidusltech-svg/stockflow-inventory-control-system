package com.stockflow.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByProductIdAndBatchNumberIgnoreCase(Long productId, String batchNumber);
}
