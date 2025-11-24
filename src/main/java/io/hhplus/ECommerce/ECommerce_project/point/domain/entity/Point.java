package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.point.domain.enums.PointType;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "points")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Point extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "used_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal usedAmount;  // 사용된 금액 (부분 사용 지원)

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false)
    private PointType pointType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;  // 포인트 만료일

    @Column(name = "used_at")
    private LocalDateTime usedAt;  // 포인트 마지막 사용 시각

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 논리적 삭제

    @Column(name = "is_expired")
    private boolean isExpired;  // 만료 여부 (쿼리 최적화)

    @Column(name = "is_used")
    private boolean isUsed;  // 전액 사용 완료 여부 (쿼리 최적화)

    // ===== 정적 팩토리 메서드 =====

    /**
     * 포인트 충전
     */
    public static Point charge(User user, BigDecimal amount, String description) {
        validateUser(user);
        validateAmount(amount);

        LocalDateTime expiredAt = LocalDateTime.now().plusYears(1);  // 1년 후 만료

        return new Point(
            null,   // version (JPA 관리)
            user,
            amount,
            BigDecimal.ZERO,  // usedAmount (초기값 0)
            PointType.CHARGE,
            description,
            null,   // createdAt (@CreationTimestamp가 자동 설정)
            null,   // updatedAt (@UpdateTimestamp가 자동 설정)
            expiredAt,
            null,   // usedAt (아직 미사용)
            null,   // deletedAt (삭제 안됨)
            false,  // isExpired
            false   // isUsed
        );
    }

    /**
     * 포인트 환불 (주문 취소 시)
     */
    public static Point refund(User user, BigDecimal amount, String description) {
        validateUser(user);
        validateAmount(amount);

        LocalDateTime expiredAt = LocalDateTime.now().plusYears(1);

        return new Point(
            null,   // version (JPA 관리)
            user,
            amount,
            BigDecimal.ZERO,  // usedAmount (초기값 0)
            PointType.REFUND,
            description,
            null,   // createdAt
            null,   // updatedAt
            expiredAt,
            null,   // usedAt (아직 미사용)
            null,   // deletedAt
            false,  // isExpired
            false   // isUsed
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 포인트 만료 처리
     */
    public void expire() {
        if (this.isUsed) {
            throw new PointException(ErrorCode.POINT_CANNOT_EXPIRE_USED_POINT);
        }

        if (this.isExpired) {
            throw new PointException(ErrorCode.POINT_ALREADY_EXPIRED);
        }

        if (this.expiredAt == null) {
            throw new PointException(ErrorCode.POINT_NO_EXPIRATION_DATE);
        }

        this.isExpired = true;
    }

    /**
     * 포인트 사용 처리 (부분 사용 및 전액 사용 모두 지원)
     * @param amountToUse 사용할 금액
     */
    public void usePartially(BigDecimal amountToUse) {
        // NOTE: 현재는 CHARGE와 REFUND만 존재하므로 타입 검증 불필요
        /*
        if (this.pointType != PointType.CHARGE && this.pointType != PointType.REFUND) {
            throw new PointException(ErrorCode.POINT_ONLY_CHARGE_OR_REFUND_CAN_BE_USED);
        }
         */

        if (this.isUsed) {
            throw new PointException(ErrorCode.POINT_ALREADY_USED);
        }

        if (this.isExpired) {
            throw new PointException(ErrorCode.POINT_EXPIRED_CANNOT_USE);
        }

        if (this.expiredAt != null && LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new PointException(ErrorCode.POINT_EXPIRATION_DATE_PASSED);
        }

        if (amountToUse == null || amountToUse.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        // 사용 가능한 잔액 확인
        BigDecimal remainingAmount = this.amount.subtract(this.usedAmount);
        if (amountToUse.compareTo(remainingAmount) > 0) {
            throw new PointException(ErrorCode.POINT_INSUFFICIENT_POINT);
        }

        // 사용 시각 업데이트 (매번 갱신)
        this.usedAt = LocalDateTime.now();

        // 사용 금액 누적
        this.usedAmount = this.usedAmount.add(amountToUse);

        // 전액 사용된 경우 isUsed = true
        if (this.usedAmount.compareTo(this.amount) == 0) {
            this.isUsed = true;
        }
    }

    /**
     * 포인트 삭제 (논리적 삭제)
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new PointException(ErrorCode.POINT_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 남은 사용 가능한 포인트 금액 계산
     */
    public BigDecimal getRemainingAmount() {
        if (this.usedAmount == null) {
            return this.amount;
        }
        return this.amount.subtract(this.usedAmount);
    }

    /**
     * 포인트 사용 취소 (보상 트랜잭션용)
     * @param amountToRestore 복구할 금액
     */
    public void restoreUsedAmount(BigDecimal amountToRestore) {
        // NOTE: 현재는 CHARGE와 REFUND만 존재하므로 타입 검증 불필요
        /*
        if (this.pointType != PointType.CHARGE && this.pointType != PointType.REFUND) {
            throw new PointException(ErrorCode.POINT_ONLY_CHARGE_OR_REFUND_CAN_BE_USED);
        }
         */

        if (amountToRestore == null || amountToRestore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        // 복구할 금액이 사용된 금액보다 크면 안됨
        if (amountToRestore.compareTo(this.usedAmount) > 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }

        // 사용 금액 감소
        this.usedAmount = this.usedAmount.subtract(amountToRestore);

        // 전액 사용 상태였다면 해제
        if (this.isUsed && this.usedAmount.compareTo(this.amount) < 0) {
            this.isUsed = false;
        }
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 사용 가능한 포인트인지 확인
     * - 부분 사용된 포인트도 잔액이 남아있으면 사용 가능
     * - 전액 사용된 경우 (isUsed = true) 사용 불가
     */
    public boolean isAvailable() {
        if (this.isUsed || this.isExpired) {
            return false;  // 전액 사용되었거나 만료된 경우
        }

        if (this.expiredAt != null && LocalDateTime.now().isAfter(this.expiredAt)) {
            return false;  // 만료일이 지난 경우
        }

        // 부분 사용되어도 잔액이 남아있으면 사용 가능
        return getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 포인트가 만료되었는지 확인
     */
    public boolean checkExpired() {
        if (this.expiredAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    /**
     * 충전 포인트인지 확인
     */
    public boolean isChargeType() {
        return this.pointType == PointType.CHARGE;
    }

    /**
     * 환불 포인트인지 확인
     */
    public boolean isRefundType() {
        return this.pointType == PointType.REFUND;
    }

    // ===== Validation 메서드 =====

    private static void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new PointException(ErrorCode.USER_ID_REQUIRED);
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new PointException(ErrorCode.POINT_AMOUNT_REQUIRED);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }
    }
}
