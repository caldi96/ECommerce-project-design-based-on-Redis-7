package io.hhplus.ECommerce.ECommerce_project.order.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetOrderDetailResponse(
        // 주문 기본 정보
        Long orderId,
        Long userId,
        OrderStatus orderStatus,

        // 금액 정보
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal pointAmount,
        BigDecimal finalAmount,

        // 쿠폰 정보
        Long couponId,

        // 날짜 정보
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,

        // 주문 항목 목록
        List<OrderItemDetail> orderItems
) {
    public static GetOrderDetailResponse of(Orders order, List<OrderItem> orderItems) {
        List<OrderItemDetail> itemDetails = orderItems.stream()
                .map(OrderItemDetail::from)
                .toList();

        return new GetOrderDetailResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getPointAmount(),
                order.getFinalAmount(),
                order.getCouponId(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getPaidAt(),
                order.getCanceledAt(),
                itemDetails
        );
    }

    public record OrderItemDetail(
            // 기본 정보
            Long orderItemId,
            Long orderId,
            Long productId,

            // 상품 정보
            String productName,

            // 수량 및 금액
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subTotal,

            // 상태 정보
            OrderItemStatus status,

            // 상태별 날짜
            LocalDateTime confirmedAt,
            LocalDateTime canceledAt,
            LocalDateTime returnedAt,
            LocalDateTime refundedAt,

            // 생성/수정 날짜
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static OrderItemDetail from(OrderItem orderItem) {
            return new OrderItemDetail(
                    orderItem.getId(),
                    orderItem.getOrderId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getSubTotal(),
                    orderItem.getStatus(),
                    orderItem.getConfirmedAt(),
                    orderItem.getCanceledAt(),
                    orderItem.getReturnedAt(),
                    orderItem.getRefundedAt(),
                    orderItem.getCreatedAt(),
                    orderItem.getUpdatedAt()
            );
        }
    }
}