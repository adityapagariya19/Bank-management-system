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

public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {

    boolean existsByReference(String reference);
    long countByCreatedAtAfter(LocalDateTime since);

    @Query("select t from BankTransaction t where (t.fromAccount.id in :ids or t.toAccount.id in :ids) and t.createdAt >= :from and t.createdAt < :to order by t.createdAt desc, t.id desc")
    Page<BankTransaction> findForAccounts(@Param("ids") Collection<Long> ids, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select t from BankTransaction t where (t.fromAccount.id in :ids or t.toAccount.id in :ids) and t.createdAt >= :since order by t.createdAt asc")
    List<BankTransaction> findSince(@Param("ids") Collection<Long> ids, @Param("since") LocalDateTime since);

    @Query("select coalesce(sum(t.amount), 0) from BankTransaction t where t.type = :type and t.fromAccount.owner.id = :uid and t.createdAt >= :since")
    BigDecimal sumSince(@Param("uid") Long uid, @Param("type") TxType type, @Param("since") LocalDateTime since);

    Page<BankTransaction> findByPerformedById(Long userId, Pageable pageable);
}
