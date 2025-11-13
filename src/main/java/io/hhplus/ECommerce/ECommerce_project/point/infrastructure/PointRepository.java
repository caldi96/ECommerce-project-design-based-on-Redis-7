package io.hhplus.ECommerce.ECommerce_project.point.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {

    // 유저의 사용가능한 포인트만 가져옴
    @Query("""
            SELECT p FROM Point p
            WHERE p.user.id = :userId
            AND p.isUsed = false
            AND p.isExpired = false
            AND (p.expiredAt IS NULL OR p.expiredAt > CURRENT_TIMESTAMP)
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt ASC
            """)
    List<Point> findAvailablePointsByUserId(@Param("userId") Long userId);

    // 포인트 목록 조회 (정렬, 페이징)
    @Query("""
            SELECT p
            FROM Point p
            WHERE p.user.id = :userId
            AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    Page<Point> findByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    // 포인트 목록 개수 조회
    long countByUserIdAndDeletedAtIsNull(Long userId);

    // 포인트 조회 (비관적 락)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Point p WHERE p.id = :pointId AND p.deletedAt IS NULL")
    Optional<Point> findByIdWithLock(@Param("pointId") Long pointId);
}
