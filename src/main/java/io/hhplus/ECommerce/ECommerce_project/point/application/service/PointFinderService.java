package io.hhplus.ECommerce.ECommerce_project.point.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointFinderService {

    private final PointRepository pointRepository;

    /**
     * 포인트 조회
     */
    public Point getPoint(Long pointId) {
        return pointRepository.findById(pointId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));
    }

    /**
     * 포인트 조회 (비관적 락)
     */
    public Point getPointWithLock(Long pointId) {
        return pointRepository.findByIdWithLock(pointId)
                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));
    }

    /**
     * 유효한 포인트 목록 조회
     */
    public List<Point> getAvailablePoints(Long userId) {
        return pointRepository.findAvailablePointsByUserId(userId);
    }

    /**
     * 포인트 이력 조회 (페이징)
     */
    public Page<Point> getPointHistoryPage(Long userId, Pageable pageable) {
        return pointRepository.findByUserIdWithPaging(userId, pageable);
    }
}
