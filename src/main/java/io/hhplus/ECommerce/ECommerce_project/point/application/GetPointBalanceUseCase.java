package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointBalanceCommand;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointCalculateService;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointBalanceResponse;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointBalanceUseCase {

    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final PointFinderService pointFinderService;
    private final PointCalculateService pointCalculateService;

    @Transactional(readOnly = true)
    public GetPointBalanceResponse execute(GetPointBalanceCommand command) {

        // 1. ID 검증
        userDomainService.validateId(command.userId());

        // 2. 사용자 조회
        User user = userFinderService.getUser(command.userId());

        // 3. 사용 가능한 포인트 목록 조회
        List<Point> availablePoints = pointFinderService.getAvailablePoints(command.userId());

        // 4. 총 잔액 계산
        BigDecimal totalBalance = pointCalculateService.calculateTotalBalance(availablePoints);

        // 5. Response 반환
        return GetPointBalanceResponse.of(user.getId(), totalBalance);
    }
}
