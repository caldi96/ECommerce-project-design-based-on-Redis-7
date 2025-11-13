package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointBalanceCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointBalanceResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointBalanceUseCase {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public GetPointBalanceResponse execute(GetPointBalanceCommand command) {
        // 1. 사용자 조회
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 사용 가능한 포인트 목록 조회
        List<Point> availablePointList = pointRepository.findAvailablePointsByUserId(command.userId());

        // 3. 총 잔액 계산
        BigDecimal totalBalance = availablePointList.stream()
                .map(Point::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Response 반환
        return GetPointBalanceResponse.of(user.getId(), totalBalance);
    }
}
