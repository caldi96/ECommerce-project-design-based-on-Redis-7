package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointCompensationService {

    private final PointUsageHistoryFinderService pointUsageHistoryFinderService;
    private final PointFinderService pointFinderService;
    private final UserFinderService userFinderService;

    /**
     * 주문 취소/결제 실패 시 포인트 복구
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void compensate(Long orderId, Long userId) {
        // 1. 포인트 사용 이력 조회
        List<PointUsageHistory> histories =
                pointUsageHistoryFinderService.getPointUsageHistories(orderId);

        BigDecimal totalRestoredPoint = BigDecimal.ZERO;

        // 2. 각 포인트별로 복구
        for (PointUsageHistory history : histories) {
            // 원본 포인트 조회 (낙관적 락 적용)
            Point originalPoint = pointFinderService.getPoint(history.getPoint().getId());

            // 사용한 포인트 금액만큼 복구
            originalPoint.restoreUsedAmount(history.getUsedAmount());

            // 포인트 사용 이력 취소 처리
            history.cancel();

            totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
        }

        // 3. 사용자 포인트 잔액 복구
        if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
            User user = userFinderService.getUser(userId);
            user.refundPoint(totalRestoredPoint);
        }

    }
}
