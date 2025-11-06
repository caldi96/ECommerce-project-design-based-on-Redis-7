package io.hhplus.ECommerce.ECommerce_project.point.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;

import java.util.List;
import java.util.Optional;

public interface PointUsageHistoryRepository {

    /**
     * 포인트 사용 이력 저장
     */
    PointUsageHistory save(PointUsageHistory pointUsageHistory);

    /**
     * ID로 조회
     */
    Optional<PointUsageHistory> findById(Long id);

    /**
     * 주문 ID로 취소되지 않은 포인트 사용 이력 조회
     * (주문 취소 시 사용)
     */
    List<PointUsageHistory> findByOrderIdAndCanceledAtIsNull(Long orderId);

    /**
     * 포인트 ID로 사용 이력 조회
     */
    List<PointUsageHistory> findByPointId(Long pointId);

    /**
     * 주문 ID로 모든 사용 이력 조회 (취소 포함)
     */
    List<PointUsageHistory> findByOrderId(Long orderId);

    /**
     * 전체 조회
     */
    List<PointUsageHistory> findAll();
}