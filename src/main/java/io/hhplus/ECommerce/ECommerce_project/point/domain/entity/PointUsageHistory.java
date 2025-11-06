package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포인트 사용 이력 엔티티
 * Point와 Order의 N:M 관계를 관리하는 중간 테이블
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PointUsageHistory {

    private Long id;

    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "point_id")
    // private Point point;
    private Long pointId;  // 사용된 원본 포인트 (CHARGE 또는 REFUND 타입)

    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id")
    // private Order order;
    private Long orderId;  // 주문 ID

    private BigDecimal usedAmount;  // 이 주문에서 이 포인트로 사용한 금액
    private LocalDateTime createdAt;  // 사용 일시
    private LocalDateTime canceledAt;  // 취소 일시 (null이면 취소 안됨)

    // ===== 정적 팩토리 메서드 =====

    /**
     * 포인트 사용 이력 생성
     */
    public static PointUsageHistory create(Long pointId, Long orderId, BigDecimal usedAmount) {
        validatePointId(pointId);
        validateOrderId(orderId);
        validateUsedAmount(usedAmount);

        LocalDateTime now = LocalDateTime.now();

        return new PointUsageHistory(
            null,  // id는 저장 시 생성
            pointId,
            orderId,
            usedAmount,
            now,
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

    private static void validatePointId(Long pointId) {
        if (pointId == null) {
            throw new PointException(ErrorCode.POINT_NOT_FOUND, "포인트 ID는 필수입니다.");
        }
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
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

    // ===== 테스트를 위한 ID 설정 메서드 (인메모리 DB용) =====
    public void setId(Long id) {
        this.id = id;
    }
}