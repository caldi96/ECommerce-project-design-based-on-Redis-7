package io.hhplus.ECommerce.ECommerce_project.order.application.command;

public record CancelOrderCommand(
        Long userId,
        Long orderId
) {
}