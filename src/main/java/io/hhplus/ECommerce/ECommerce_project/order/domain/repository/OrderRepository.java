package io.hhplus.ECommerce.ECommerce_project.order.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Orders save(Orders orders);

    Optional<Orders> findById(Long id);

    List<Orders> findAll();

    void deletedById(Long id);

    /**
     * 사용자별 주문 목록 조회 (페이징, 상태 필터링)
     */
    List<Orders> findByUserId(Long userId, OrderStatus orderStatus, int page, int size);

    /**
     * 사용자별 주문 총 개수 조회 (상태 필터링)
     */
    long countByUserId(Long userId, OrderStatus orderStatus);
}
