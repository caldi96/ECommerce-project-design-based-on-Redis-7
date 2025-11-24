package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.point.application.command.ChargePointCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.service.PointDomainService;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargePointUseCase {

    private final PointRepository pointRepository;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final PointDomainService pointDomainService;

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )

    @Transactional
    public Point execute(ChargePointCommand command) {

        // 1. ID 검증
        userDomainService.validateId(command.userId());

        // 2. 충전 포인트 금액 검증
        pointDomainService.validateAmount(command.amount());

        // 3. 유저 존재 유무 확인 및 조회
        User user = userFinderService.getUser(command.userId());

        // 4. Point 엔티티 생성 및 저장
        Point point = Point.charge(
                user,
                command.amount(),
                command.description()
        );
        Point savedPoint = pointRepository.save(point);

        // 5. User의 포인트 잔액 업데이트
        user.chargePoint(command.amount());

        // 6. 저장된 포인트 반환
        return savedPoint;
    }
}
