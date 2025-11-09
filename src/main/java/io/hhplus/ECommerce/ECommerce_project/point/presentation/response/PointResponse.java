package io.hhplus.ECommerce.ECommerce_project.point.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.enums.PointType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PointResponse(
        Long id,
        BigDecimal amount,
        BigDecimal usedAmount,
        PointType pointType,
        String description,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean isExpired,
        boolean isUsed
) {
    public static PointResponse from(Point point) {
        return new PointResponse(
                point.getId(),
                point.getAmount(),
                point.getUsedAmount(),
                point.getPointType(),
                point.getDescription(),
                point.getCreatedAt(),
                point.getExpiresAt(),
                point.isExpired(),
                point.isUsed()
        );
    }

    public static List<PointResponse> from(List<Point> pointList) {
        return pointList.stream()
                .map(PointResponse::from)
                .toList();
    }
}
