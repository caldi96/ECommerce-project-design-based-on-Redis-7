package io.hhplus.ECommerce.ECommerce_project.order.application.command;

import java.math.BigDecimal;

public record CreateOrderFromProductCommand(
        Long userId,
        Long productId,         // 주문할 상품 ID
        Integer quantity,       // 주문 수량
        BigDecimal pointAmount, // 사용할 포인트 (선택, null 가능)
        Long couponId          // 사용할 쿠폰 ID (선택, null 가능)
) {}