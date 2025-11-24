package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFinderService {

    private final OrderRepository orderRepository;

    /**
     * 주문 단건 조회 (비관적 락)
     */
    public Orders getOrderWithLock(Long orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 주문 단건 조회
     */
    public Orders getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 유저 주문 목록 조회(페이징 처리)
     */
    public Page<Orders> getOrdersPageByUserId(Long userId, OrderStatus orderStatus, Pageable pageable) {
        return orderRepository.findByUserIdWithPaging(
                userId,
                orderStatus,
                pageable
        );
    }
}
