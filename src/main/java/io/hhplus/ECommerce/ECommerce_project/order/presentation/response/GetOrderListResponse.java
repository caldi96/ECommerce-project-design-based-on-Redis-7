package io.hhplus.ECommerce.ECommerce_project.order.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetOrderListResponse(
        List<OrderSummary> orders,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static GetOrderListResponse of(List<Orders> orders, int page, int size, long totalElements) {
        List<OrderSummary> orderSummaries = orders.stream()
                .map(OrderSummary::from)
                .toList();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new GetOrderListResponse(
                orderSummaries,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public record OrderSummary(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            BigDecimal shippingFee,
            BigDecimal discountAmount,
            BigDecimal pointAmount,
            BigDecimal finalAmount,
            OrderStatus orderStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static OrderSummary from(Orders order) {
            return new OrderSummary(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getShippingFee(),
                    order.getDiscountAmount(),
                    order.getPointAmount(),
                    order.getFinalAmount(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }
}