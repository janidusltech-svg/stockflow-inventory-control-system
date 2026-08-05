package com.stockflow.issuing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockIssueRepository extends JpaRepository<StockIssue, Long> {

    boolean existsByIssueNumberIgnoreCase(String issueNumber);

    Optional<StockIssue> findTopByOrderByIdDesc();

    @Query("""
            select distinct issue
            from StockIssue issue
            join fetch issue.recipient recipient
            left join fetch issue.items items
            left join fetch items.product product
            left join fetch product.unit
            left join fetch items.location location
            left join fetch location.warehouse
            left join fetch items.batch
            order by issue.issueDate desc, issue.id desc
            """)
    List<StockIssue> findAllWithDetails();

    @Query("""
            select distinct issue
            from StockIssue issue
            join fetch issue.recipient recipient
            left join fetch issue.items items
            left join fetch items.product product
            left join fetch product.unit
            left join fetch items.location location
            left join fetch location.warehouse
            left join fetch items.batch
            where issue.id = :id
            """)
    Optional<StockIssue> findByIdWithDetails(@Param("id") Long id);
}
