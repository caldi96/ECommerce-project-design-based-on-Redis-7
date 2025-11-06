package io.hhplus.ECommerce.ECommerce_project.point.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.point.application.command.ChargePointCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChargePointRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @NotNull(message = "포인트는 필수입니다")
        @DecimalMin(value = "1.0", inclusive = true, message = "충전 포인트는 0 이상이어야 합니다")
        BigDecimal amount,

        String description
) {

    public ChargePointCommand toCommand() {
        return new ChargePointCommand(
                userId,
                amount,
                description
        );
    }
}
