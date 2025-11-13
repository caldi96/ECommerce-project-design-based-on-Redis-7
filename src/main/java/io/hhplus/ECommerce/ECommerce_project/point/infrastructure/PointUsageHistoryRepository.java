package io.hhplus.ECommerce.ECommerce_project.point.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUsageHistoryRepository extends JpaRepository<PointUsageHistory, Long> {

    // 주문 id로 포인트 사용 목록 조회(취소되지 않은 것들만)
    List<PointUsageHistory> findByOrders_IdAndCanceledAtIsNull(Long orderId);

    // 포인트 id로 포인트 사용 목록 조회(취소되지 않은 것들만)
    List<PointUsageHistory> findByPoint_IdAndCanceledAtIsNull(Long pointId);
}
