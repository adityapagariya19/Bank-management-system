package com.meridian.repo;

import com.meridian.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where :q = '' or lower(a.action) like lower(concat('%', :q, '%')) or lower(a.actorEmail) like lower(concat('%', :q, '%')) or lower(a.details) like lower(concat('%', :q, '%')) order by a.createdAt desc, a.id desc")
    Page<AuditLog> search(@Param("q") String q, Pageable pageable);
    List<AuditLog> findTop10ByActorIdAndActionInOrderByCreatedAtDesc(Long actorId, Collection<String> actions);
}
