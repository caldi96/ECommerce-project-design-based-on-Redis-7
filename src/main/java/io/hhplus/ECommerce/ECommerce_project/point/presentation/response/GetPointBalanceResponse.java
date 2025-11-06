package io.hhplus.ECommerce.ECommerce_project.point.presentation.response;

import java.math.BigDecimal;

public record GetPointBalanceResponse(
        Long userId,
        BigDecimal totalBalance
) {
    public static GetPointBalanceResponse of(Long userId, BigDecimal totalBalance) {
        return new GetPointBalanceResponse(userId, totalBalance);
    }
}