package io.hhplus.ECommerce.ECommerce_project.point.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;

import java.util.List;
import java.util.Optional;

public interface PointRepository {

    Point save(Point point);

    Optional<Point> findById(Long id);

    List<Point> findAll();

    List<Point> findAvailablePointsByUserId(Long userId);

    /**
     * 사용자의 포인트 이력 조회 (페이징)
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 포인트 이력 목록 (최신순)
     */
    List<Point> findByUserIdWithPaging(Long userId, int page, int size);

    /**
     * 사용자의 전체 포인트 이력 개수
     * @param userId 사용자 ID
     * @return 전체 이력 개수
     */
    long countByUserId(Long userId);

    void deleteById(Long id);
}
