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

public interface UpiRepository extends JpaRepository<UpiId, Long> {

    Optional<UpiId> findByVpa(String vpa);
    boolean existsByVpa(String vpa);
    List<UpiId> findByUserIdOrderByPrimaryVpaDescIdAsc(Long userId);
    Optional<UpiId> findByUserIdAndPrimaryVpaTrue(Long userId);
    List<UpiId> findByAccountId(Long accountId);
}
