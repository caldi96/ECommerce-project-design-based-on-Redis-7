package io.hhplus.ECommerce.ECommerce_project.order.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 주문 Id 로 주문항목 가져옴
    List<OrderItem> findByOrders_Id(Long orderId);
}
