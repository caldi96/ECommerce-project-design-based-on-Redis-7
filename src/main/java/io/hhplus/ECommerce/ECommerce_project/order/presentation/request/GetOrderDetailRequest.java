package io.hhplus.ECommerce.ECommerce_project.order.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderDetailCommand;
import jakarta.validation.constraints.NotNull;

public record GetOrderDetailRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        Long orderId,

        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId
) {
    public GetOrderDetailCommand toCommand() {
        return new GetOrderDetailCommand(orderId, userId);
    }
}