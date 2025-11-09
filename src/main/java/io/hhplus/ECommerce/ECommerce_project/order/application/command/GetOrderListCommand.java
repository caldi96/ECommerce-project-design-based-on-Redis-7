package io.hhplus.ECommerce.ECommerce_project.order.application.command;

import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;

public record GetOrderListCommand(
        Long userId,
        OrderStatus orderStatus,  // 선택: 특정 상태 필터링 (null이면 전체)
        int page,                 // 페이지 번호 (0부터 시작)
        int size                  // 페이지 크기
) {
    public GetOrderListCommand {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;  // 기본값
        }
    }
}