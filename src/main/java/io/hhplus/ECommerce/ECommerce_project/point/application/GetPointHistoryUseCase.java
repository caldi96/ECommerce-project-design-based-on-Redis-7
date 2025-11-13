package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand;
import io.hhplus.ECommerce.ECommerce_project.point.application.dto.PointPageResult;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PointPageResult execute(GetPointHistoryCommand command) {
        // 1. 사용자 존재 확인
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. Pageable 생성
        Pageable pageable = PageRequest.of(command.page(), command.size());

        // 3. 포인트 이력 조회 (페이징)
        Page<Point> pointHistoryPage = pointRepository.findByUserIdWithPaging(command.userId(), pageable);

        // 4. PointPageResult 반환 (Page 객체의 계산된 값 활용)
        return new PointPageResult(
                pointHistoryPage.getContent(),
                pointHistoryPage.getNumber(),
                pointHistoryPage.getSize(),
                pointHistoryPage.getTotalElements(),
                pointHistoryPage.getTotalPages(),
                pointHistoryPage.isFirst(),
                pointHistoryPage.isLast()
        );
    }
}