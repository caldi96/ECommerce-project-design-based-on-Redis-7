package io.hhplus.ECommerce.ECommerce_project.point.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class PointUsageHistoryMemoryRepository implements PointUsageHistoryRepository {

    private final Map<Long, PointUsageHistory> historyMap = new ConcurrentHashMap<>();
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public PointUsageHistory save(PointUsageHistory pointUsageHistory) {
        // ID가 없으면 Snowflake ID 생성
        if (pointUsageHistory.getId() == null) {
            pointUsageHistory.setId(idGenerator.nextId());
        }
        historyMap.put(pointUsageHistory.getId(), pointUsageHistory);
        return pointUsageHistory;
    }

    @Override
    public Optional<PointUsageHistory> findById(Long id) {
        return Optional.ofNullable(historyMap.get(id));
    }

    @Override
    public List<PointUsageHistory> findByOrderIdAndCanceledAtIsNull(Long orderId) {
        return historyMap.values().stream()
                .filter(history -> history.getOrderId().equals(orderId))
                .filter(history -> !history.isCanceled())
                .toList();
    }

    @Override
    public List<PointUsageHistory> findByPointId(Long pointId) {
        return historyMap.values().stream()
                .filter(history -> history.getPointId().equals(pointId))
                .toList();
    }

    @Override
    public List<PointUsageHistory> findByOrderId(Long orderId) {
        return historyMap.values().stream()
                .filter(history -> history.getOrderId().equals(orderId))
                .toList();
    }

    @Override
    public List<PointUsageHistory> findAll() {
        return historyMap.values().stream().toList();
    }
}