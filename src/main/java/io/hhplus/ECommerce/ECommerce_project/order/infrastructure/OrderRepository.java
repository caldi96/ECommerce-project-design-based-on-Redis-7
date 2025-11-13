package io.hhplus.ECommerce.ECommerce_project.order.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    // 유저 주문 목록 조회(페이징 처리)
    @Query("""
            SELECT o
            FROM Orders o
            WHERE o.user.id = :userId
            AND (:orderStatus IS NULL OR o.status = :orderStatus)
            ORDER BY o.createdAt DESC
            """)
    Page<Orders> findByUserIdWithPaging(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    // 주문 상태별 유저의 주문 목록 조회
    @Query("""
    SELECT COUNT(o)
    FROM Orders o
    WHERE o.user.id = :userId
    AND (:orderStatus IS NULL OR o.status = :orderStatus)
    """)
    long countByUserId(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus
    );

    // 주문 조회(비관적 락)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orders o WHERE o.id = :orderId")
    Optional<Orders> findByIdWithLock(@Param("orderId") Long orderId);
}
