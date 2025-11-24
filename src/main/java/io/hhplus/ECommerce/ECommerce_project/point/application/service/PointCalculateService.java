package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PointCalculateService {

    public BigDecimal calculateTotalBalance(List<Point> points) {
        return points.stream()
                .map(Point::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
