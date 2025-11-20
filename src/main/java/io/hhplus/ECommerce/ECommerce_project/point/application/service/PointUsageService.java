package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointUsageService {

    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;
    private final UserRepository userRepository;
    private final PointFinderService pointFinderService;
    private final UserFinderService userFinderService;

    /**
     * 포인트 사용 처리 및 사용 이력 생성
     * @param userId 사용자 ID
     * @param pointAmountToUse 사용할 포인트 금액
     * @param order 주문 엔티티
     * @return 사용된 포인트 금액
     */
    @Transactional
    public BigDecimal usePoints(Long userId, BigDecimal pointAmountToUse, Orders order) {
        if (pointAmountToUse == null || pointAmountToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 1. 사용 가능한 포인트 조회
        List<Point> availablePoints = pointFinderService.getAvailablePoints(userId);

        // 2. 선입선출 방식으로 포인트 사용 처리
        BigDecimal remainingPointToUse = pointAmountToUse;
        List<Point> usedPoints = new ArrayList<>();
        List<BigDecimal> usageAmounts = new ArrayList<>();

        for (Point point : availablePoints) {
            if (remainingPointToUse.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal availableAmount = point.getRemainingAmount();
            BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

            usageAmounts.add(pointToUse);
            usedPoints.add(point);

            remainingPointToUse = remainingPointToUse.subtract(pointToUse);
        }

        // 3. 포인트 사용 처리 및 이력 생성
        for (int i = 0; i < usedPoints.size(); i++) {
            Point point = usedPoints.get(i);
            BigDecimal usageAmount = usageAmounts.get(i);

            // 포인트 부분 사용
            point.usePartially(usageAmount);
            pointRepository.save(point); // 테스트 코드 dirty check 오류때문에 추가

            // 사용 이력 생성
            PointUsageHistory history = PointUsageHistory.create(point, order, usageAmount);
            pointUsageHistoryRepository.save(history);
        }

        // 4. User 포인트 잔액 차감
        User user = userFinderService.getUser(userId);
        user.usePoint(pointAmountToUse);
        userRepository.save(user); // 테스트 코드 dirty check 오류때문에 추가

        return pointAmountToUse;
    }
}