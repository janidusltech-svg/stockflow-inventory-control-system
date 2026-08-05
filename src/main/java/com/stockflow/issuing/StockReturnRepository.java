package com.stockflow.issuing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockReturnRepository extends JpaRepository<StockReturn, Long> {

    boolean existsByReturnNumberIgnoreCase(String returnNumber);
    boolean existsByStockIssue_IdAndStatus(Long stockIssueId, StockReturnStatus status);
    Optional<StockReturn> findTopByOrderByIdDesc();

    @Query("""
            select distinct returned
            from StockReturn returned
            join fetch returned.stockIssue issue
            join fetch issue.recipient
            left join fetch returned.items items
            left join fetch items.issueItem issueItem
            left join fetch issueItem.product product
            left join fetch product.unit
            left join fetch issueItem.location location
            left join fetch location.warehouse
            left join fetch issueItem.batch
            order by returned.returnDate desc, returned.id desc
            """)
    List<StockReturn> findAllWithDetails();

    @Query("""
            select distinct returned
            from StockReturn returned
            join fetch returned.stockIssue issue
            join fetch issue.recipient
            left join fetch returned.items items
            left join fetch items.issueItem issueItem
            left join fetch issueItem.product product
            left join fetch product.unit
            left join fetch issueItem.location location
            left join fetch location.warehouse
            left join fetch issueItem.batch
            where returned.id = :id
            """)
    Optional<StockReturn> findByIdWithDetails(@Param("id") Long id);
}
