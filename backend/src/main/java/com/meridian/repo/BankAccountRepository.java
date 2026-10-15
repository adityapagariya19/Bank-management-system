package com.meridian.repo;

import com.meridian.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByOwnerId(Long ownerId);
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    Optional<BankAccount> findByAccountNumberAndBranch_Ifsc(String accountNumber, String ifsc);
    long countByStatus(AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(a.balance), 0) from BankAccount a")
    BigDecimal totalBalance();

    @Query("select b.name, b.color, count(a), coalesce(sum(a.balance), 0) from Bank b left join BankAccount a on a.bank.id = b.id group by b.id, b.name, b.color order by b.name")
    List<Object[]> bankSummary();
}
