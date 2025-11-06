package io.hhplus.ECommerce.ECommerce_project.order.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.GetOrderListCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

public record GetOrderListRequest(
        Long userId,
        OrderStatus orderStatus,
        Integer page,
        Integer size
) {
    public GetOrderListCommand toCommand() {
        return new GetOrderListCommand(
                userId,
                orderStatus,
                page != null ? page : 0,
                size != null ? size : 10
        );
    }
}