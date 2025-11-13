package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포인트 사용 이력 엔티티
 * Point와 Order의 N:M 관계를 관리하는 중간 테이블
 */
@Entity
@Table(name = "point_usage_histories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PointUsageHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    @Column(name = "used_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal usedAmount;  // 이 주문에서 이 포인트로 사용한 금액

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 사용 일시

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;  // 취소 일시 (null이면 취소 안됨)

    // ===== 정적 팩토리 메서드 =====

    /**
     * 포인트 사용 이력 생성
     */
    public static PointUsageHistory create(
            Point point,
            Orders orders,
            BigDecimal usedAmount) {
        validatePointId(point);
        validateOrderId(orders);
        validateUsedAmount(usedAmount);

        LocalDateTime now = LocalDateTime.now();

        return new PointUsageHistory(
            point,
            orders,
            usedAmount,
            null,
            null  // canceledAt (취소되지 않음)
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 포인트 사용 취소
     */
    public void cancel() {
        if (this.canceledAt != null) {
            throw new PointException(ErrorCode.POINT_ALREADY_USED,
                "이미 취소된 포인트 사용 이력입니다.");
        }

        this.canceledAt = LocalDateTime.now();
    }

    /**
     * 취소 여부 확인
     */
    public boolean isCanceled() {
        return this.canceledAt != null;
    }

    /**
     * 유효한 사용 이력인지 확인 (취소되지 않음)
     */
    public boolean isValid() {
        return this.canceledAt == null;
    }

    // ===== Validation 메서드 =====

    private static void validatePointId(Point point) {
        if (point == null || point.getId() == null) {
            throw new PointException(ErrorCode.POINT_NOT_FOUND, "포인트 ID는 필수입니다.");
        }
    }

    private static void validateOrderId(Orders orders) {
        if (orders == null || orders.getId() == null) {
            throw new PointException(ErrorCode.POINT_ORDER_ID_REQUIRED);
        }
    }

    private static void validateUsedAmount(BigDecimal usedAmount) {
        if (usedAmount == null) {
            throw new PointException(ErrorCode.POINT_AMOUNT_REQUIRED);
        }
        if (usedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID,
                "사용 금액은 0보다 커야 합니다.");
        }
    }
}