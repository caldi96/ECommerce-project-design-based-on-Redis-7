package io.hhplus.ECommerce.ECommerce_project.order.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderFromProductRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @NotNull(message = "상품 ID는 필수입니다")
        Long productId,

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        Integer quantity,

        BigDecimal pointAmount,  // 사용할 포인트 (선택, null 가능)

        Long couponId           // 사용할 쿠폰 ID (선택, null 가능)
) {
    public CreateOrderFromProductCommand toCommand() {
        return new CreateOrderFromProductCommand(
                userId,
                productId,
                quantity,
                pointAmount,
                couponId
        );
    }
}