package com.stockflow.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String sku,
            String name,
            String category
    );
}
