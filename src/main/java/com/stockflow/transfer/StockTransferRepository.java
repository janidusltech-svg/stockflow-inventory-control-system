package com.stockflow.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @Query("""
            select distinct transfer
            from StockTransfer transfer
            join fetch transfer.sourceLocation source
            join fetch source.warehouse
            join fetch transfer.destinationLocation destination
            join fetch destination.warehouse
            left join fetch transfer.items items
            left join fetch items.product
            left join fetch items.batch
            order by transfer.id desc
            """)
    List<StockTransfer> findAllWithDetails();

    @Query("""
            select distinct transfer
            from StockTransfer transfer
            join fetch transfer.sourceLocation source
            join fetch source.warehouse
            join fetch transfer.destinationLocation destination
            join fetch destination.warehouse
            left join fetch transfer.items items
            left join fetch items.product product
            left join fetch product.unit
            left join fetch items.batch
            where transfer.id = :id
            """)
    Optional<StockTransfer> findByIdWithDetails(@Param("id") Long id);

    Optional<StockTransfer> findTopByOrderByIdDesc();
}
