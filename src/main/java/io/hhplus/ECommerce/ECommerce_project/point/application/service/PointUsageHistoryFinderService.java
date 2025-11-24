package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointUsageHistoryFinderService {

    private final PointUsageHistoryRepository pointUsageHistoryRepository;

    public List<PointUsageHistory> getPointUsageHistories(Long orderId) {
        return pointUsageHistoryRepository.findByOrders_IdAndCanceledAtIsNull(orderId);
    }
}
