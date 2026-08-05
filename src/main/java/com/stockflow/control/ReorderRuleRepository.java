package com.stockflow.control;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, Long> {

    boolean existsByProductId(Long productId);

    boolean existsByProductIdAndIdNot(Long productId, Long id);

    @Query("""
            select rule
            from ReorderRule rule
            join fetch rule.product product
            join fetch product.category
            left join fetch product.brand
            join fetch product.unit
            order by product.name
            """)
    List<ReorderRule> findAllWithProduct();

    @Query("""
            select rule
            from ReorderRule rule
            join fetch rule.product product
            join fetch product.category
            left join fetch product.brand
            join fetch product.unit
            where rule.id = :id
            """)
    Optional<ReorderRule> findByIdWithProduct(@Param("id") Long id);

    @Query("""
            select rule
            from ReorderRule rule
            join fetch rule.product product
            join fetch product.unit
            where rule.active = true and product.active = true
            order by product.name
            """)
    List<ReorderRule> findAllActiveWithProduct();
}
