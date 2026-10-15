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

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndSeenFalse(Long userId);
    @Modifying
    @Query("update Notification n set n.seen = true where n.user.id = :uid and n.seen = false")
    int markAllSeen(@Param("uid") Long uid);
}
