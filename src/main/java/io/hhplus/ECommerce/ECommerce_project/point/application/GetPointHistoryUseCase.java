package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.presentation.response.GetPointHistoryResponse;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public GetPointHistoryResponse execute(GetPointHistoryCommand command) {
        // 1. 사용자 존재 확인
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 포인트 이력 조회 (페이징)
        List<Point> pointHistory = pointRepository.findByUserIdWithPaging(
                command.userId(),
                command.page(),
                command.size()
        );

        // 3. 전체 개수 조회
        long totalElements = pointRepository.countByUserId(command.userId());

        // 4. Response 반환
        return GetPointHistoryResponse.of(
                pointHistory,
                command.page(),
                command.size(),
                totalElements
        );
    }
}