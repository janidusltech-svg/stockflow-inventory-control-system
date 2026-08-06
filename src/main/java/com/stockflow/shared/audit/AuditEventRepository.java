package com.stockflow.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    long countByOutcome(AuditOutcome outcome);

    long countByOccurredAtAfter(LocalDateTime fromDateTime);

    List<AuditEvent> findByOccurredAtAfterOrderByOccurredAtDesc(LocalDateTime fromDateTime);

    @Query("select distinct event.module from AuditEvent event order by event.module")
    List<String> findDistinctModules();

    @Query("select distinct event.action from AuditEvent event order by event.action")
    List<String> findDistinctActions();

    @Query("""
            select event
            from AuditEvent event
            where (:keyword = ''
                or lower(event.username) like lower(concat('%', :keyword, '%'))
                or lower(event.roleName) like lower(concat('%', :keyword, '%'))
                or lower(event.description) like lower(concat('%', :keyword, '%'))
                or lower(event.requestPath) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(event.referenceValue, '')) like lower(concat('%', :keyword, '%')))
              and (:module = '' or event.module = :module)
              and (:action = '' or event.action = :action)
              and (:outcome is null or event.outcome = :outcome)
            order by event.occurredAt desc
            """)
    Page<AuditEvent> search(@Param("keyword") String keyword,
                            @Param("module") String module,
                            @Param("action") String action,
                            @Param("outcome") AuditOutcome outcome,
                            Pageable pageable);
}
