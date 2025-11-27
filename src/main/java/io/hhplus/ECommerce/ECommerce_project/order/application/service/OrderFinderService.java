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

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 15분 이상 PENDING 상태인 주문 목록
     */
    public List<Orders> getExpiredOrders(OrderStatus status, LocalDateTime dateTime) {
        return orderRepository.findByStatusAndCreatedAtBefore(status, dateTime);
    }
}
