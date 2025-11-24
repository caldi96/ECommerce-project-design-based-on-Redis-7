package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemFinderService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 항목 목록 조회
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrders_Id(orderId);
    }
}
