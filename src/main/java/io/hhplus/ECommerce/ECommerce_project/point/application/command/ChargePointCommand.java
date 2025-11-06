package io.hhplus.ECommerce.ECommerce_project.point.application.command;

import java.math.BigDecimal;

public record ChargePointCommand(
        Long userId,
        BigDecimal amount,
        String description
) {}
