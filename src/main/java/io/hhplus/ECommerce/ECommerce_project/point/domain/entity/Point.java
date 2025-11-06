package io.hhplus.ECommerce.ECommerce_project.point.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.point.domain.enums.PointType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Point {

    private Long id;

    // 나중에 JPA 연결 시
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "user_id")
    //private User user;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal usedAmount;  // 사용된 금액 (부분 사용 지원)
    private PointType pointType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isExpired;  // 만료 여부
    private boolean isUsed;     // 사용 여부

    // ===== 정적 팩토리 메서드 =====

    /**
     * 포인트 충전
     */
    public static Point charge(Long userId, BigDecimal amount, String description) {
        validateUserId(userId);
        validateAmount(amount);
//        validateDescription(description);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);  // 1년 후 만료

        return new Point(
            null,  // id는 저장 시 생성
            userId,
            amount,
            BigDecimal.ZERO,  // usedAmount (초기값 0)
            PointType.CHARGE,
            description,
            now,   // createdAt
            expiresAt,
            false, // isExpired
            false  // isUsed
        );
    }

    /**
     * 포인트 사용 (주문 결제) - 사용 이력 기록용
     */
    public static Point use(Long userId, BigDecimal amount, String description) {
        validateUserId(userId);
        validateAmount(amount);
//        validateDescription(description);

        LocalDateTime now = LocalDateTime.now();

        return new Point(
            null,
            userId,
            amount,
            amount,  // usedAmount (USE 타입은 전액 사용으로 기록)
            PointType.USE,
            description,
            now,
            null,  // 사용 포인트는 만료일 없음
            false,
            true   // 사용 시점에 이미 사용됨
        );
    }

    /**
     * 포인트 환불 (주문 취소 시)
     */
    public static Point refund(Long userId, BigDecimal amount, String description) {
        validateUserId(userId);
        validateAmount(amount);
//        validateDescription(description);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);

        return new Point(
            null,
            userId,
            amount,
            BigDecimal.ZERO,  // usedAmount (초기값 0)
            PointType.REFUND,
            description,
            now,
            expiresAt,
            false,
            false
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 포인트 만료 처리
     */
    public void expire() {
        if (this.pointType == PointType.USE || this.isUsed) {
            throw new PointException(ErrorCode.POINT_CANNOT_EXPIRE_USED_POINT);
        }

        if (this.isExpired) {
            throw new PointException(ErrorCode.POINT_ALREADY_EXPIRED);
        }

        if (this.expiresAt == null) {
            throw new PointException(ErrorCode.POINT_NO_EXPIRATION_DATE);
        }

        this.isExpired = true;
    }

    /**
     * 포인트 부분 사용 처리 (사용할 금액만큼 usedAmount 증가)
     * @param amountToUse 사용할 금액
     */
    public void usePartially(BigDecimal amountToUse) {
        if (this.pointType != PointType.CHARGE && this.pointType != PointType.REFUND) {
            throw new PointException(ErrorCode.POINT_ONLY_CHARGE_OR_REFUND_CAN_BE_USED);
        }

        if (this.isUsed) {
            throw new PointException(ErrorCode.POINT_ALREADY_USED);
        }

        if (this.isExpired) {
            throw new PointException(ErrorCode.POINT_EXPIRED_CANNOT_USE);
        }

        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
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

        // 사용 금액 누적
        this.usedAmount = this.usedAmount.add(amountToUse);

        // 전액 사용된 경우 isUsed = true
        if (this.usedAmount.compareTo(this.amount) == 0) {
            this.isUsed = true;
        }
    }

    /**
     * 포인트 사용 처리 (실제 사용 시점 표시) - 전액 사용
     */
    public void markAsUsed() {
        if (this.pointType != PointType.CHARGE && this.pointType != PointType.REFUND) {
            throw new PointException(ErrorCode.POINT_ONLY_CHARGE_OR_REFUND_CAN_BE_USED);
        }

        if (this.isUsed) {
            throw new PointException(ErrorCode.POINT_ALREADY_USED);
        }

        if (this.isExpired) {
            throw new PointException(ErrorCode.POINT_EXPIRED_CANNOT_USE);
        }

        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            throw new PointException(ErrorCode.POINT_EXPIRATION_DATE_PASSED);
        }

        this.usedAmount = this.amount;
        this.isUsed = true;
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
        if (this.pointType != PointType.CHARGE && this.pointType != PointType.REFUND) {
            throw new PointException(ErrorCode.POINT_ONLY_CHARGE_OR_REFUND_CAN_BE_USED);
        }

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
        if (this.pointType == PointType.USE) {
            return false;  // 사용 타입은 사용 불가
        }

        if (this.isUsed || this.isExpired) {
            return false;  // 전액 사용되었거나 만료된 경우
        }

        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            return false;  // 만료일이 지난 경우
        }

        // 부분 사용되어도 잔액이 남아있으면 사용 가능
        return getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 포인트가 만료되었는지 확인
     */
    public boolean checkExpired() {
        if (this.expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 충전 포인트인지 확인
     */
    public boolean isChargeType() {
        return this.pointType == PointType.CHARGE;
    }

    /**
     * 사용 포인트인지 확인
     */
    public boolean isUseType() {
        return this.pointType == PointType.USE;
    }

    /**
     * 환불 포인트인지 확인
     */
    public boolean isRefundType() {
        return this.pointType == PointType.REFUND;
    }

    // ===== Validation 메서드 =====

    private static void validateUserId(Long userId) {
        if (userId == null) {
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

    /*
    private static void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new PointException(ErrorCode.POINT_DESCRIPTION_REQUIRED);
        }
    }
     */

    // ===== 테스트를 위한 ID 설정 메서드 (인메모리 DB용) =====
    public void setId(Long id) {
        this.id = id;
    }
}
