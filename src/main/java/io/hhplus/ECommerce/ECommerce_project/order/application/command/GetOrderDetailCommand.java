package io.hhplus.ECommerce.ECommerce_project.order.application.command;

public record GetOrderDetailCommand(
        Long orderId,
        Long userId  // 주문이 해당 사용자의 것인지 확인하기 위해 필요
) {
    public GetOrderDetailCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }
}