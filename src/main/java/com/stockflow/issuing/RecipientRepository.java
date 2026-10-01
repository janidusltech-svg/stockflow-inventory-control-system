package com.stockflow.issuing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Recipient> findByActiveTrueOrderByNameAsc();

    // Safe-delete checks: a recipient with any stock issue (draft included) is kept.

    @Query("select count(issue) from StockIssue issue where issue.recipient.id = :recipientId")
    long countStockIssuesByRecipientId(@Param("recipientId") Long recipientId);

    @Query("select distinct issue.recipient.id from StockIssue issue")
    List<Long> findRecipientIdsWithStockIssues();
}
