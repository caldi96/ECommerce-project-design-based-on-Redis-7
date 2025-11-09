package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.ChargePointCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ChargePointUseCase {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional
    public Point execute(ChargePointCommand command) {
        // 1. 유저 존재 유무 확인 및 조회
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 충전 포인트 금액 검증
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        // 3. Point 엔티티 생성 및 저장
        Point point = Point.charge(
                command.userId(),
                command.amount(),
                command.description()
        );
        Point savedPoint = pointRepository.save(point);

        // 4. User의 포인트 잔액 업데이트
        user.chargePoint(command.amount());
        userRepository.save(user);

        // 5. 저장된 포인트 반환
        return savedPoint;
    }
}
