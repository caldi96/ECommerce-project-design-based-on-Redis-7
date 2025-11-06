package io.hhplus.ECommerce.ECommerce_project.point.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;

import java.util.List;

public record GetPointHistoryResponse(
        List<PointResponse> pointHistory,
        int currentPage,
        int pageSize,
        long totalElements
) {
    public static GetPointHistoryResponse of(
            List<Point> points,
            int currentPage,
            int pageSize,
            long totalElements
    ) {
        List<PointResponse> pointResponses = points.stream()
                .map(PointResponse::from)
                .toList();

        return new GetPointHistoryResponse(
                pointResponses,
                currentPage,
                pageSize,
                totalElements
        );
    }
}