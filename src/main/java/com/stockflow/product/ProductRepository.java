package com.stockflow.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countByActiveTrue();

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    Optional<Product> findByBarcode(String barcode);

    long countByCategoryId(Long categoryId);

    long countByBrandId(Long brandId);

    long countByUnitId(Long unitId);

    @Query("""
            SELECT p
            FROM Product p
            JOIN FETCH p.category
            LEFT JOIN FETCH p.brand
            JOIN FETCH p.unit
            ORDER BY p.name
            """)
    List<Product> findAllWithDetails();

    @Query("""
            SELECT p
            FROM Product p
            JOIN FETCH p.category
            LEFT JOIN FETCH p.brand
            JOIN FETCH p.unit
            WHERE p.id = :id
            """)
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT p
            FROM Product p
            JOIN FETCH p.category c
            LEFT JOIN FETCH p.brand b
            JOIN FETCH p.unit u
            WHERE LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.barcode, ''))
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(b.name, ''))
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.name
            """)
    List<Product> search(@Param("keyword") String keyword);
}
