package io.hhplus.ECommerce.ECommerce_project.point.application;

import io.hhplus.ECommerce.ECommerce_project.point.application.command.GetPointHistoryCommand;
import io.hhplus.ECommerce.ECommerce_project.point.application.dto.PointPageResult;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPointHistoryUseCase {

    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final PointFinderService pointFinderService;

    @Transactional(readOnly = true)
    public PointPageResult execute(GetPointHistoryCommand command) {

        // 1. ID 검증
        userDomainService.validateId(command.userId());

        // 2. 유저 존재 유무 확인
        userFinderService.getUser(command.userId());

        // 3. Pageable 생성
        Pageable pageable = PageRequest.of(command.page(), command.size());

        // 4. 포인트 이력 조회 (페이징)
        Page<Point> pointHistoryPage = pointFinderService.getPointHistoryPage(command.userId(), pageable);

        // 5. PointPageResult 반환 (Page 객체의 계산된 값 활용)
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