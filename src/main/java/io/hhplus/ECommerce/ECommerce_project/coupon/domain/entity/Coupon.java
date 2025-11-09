package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    private Long id;
    private String name;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private int totalQuantity;              // 전체 수량
    private int issuedQuantity;             // 발급된 양
    private int usageCount;                 // 사용된 양
    private int perUserLimit;               // 인당 사용가능 양
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 쿠폰 생성
     */
    public static Coupon createCoupon(
            String name,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            Integer totalQuantity,
            Integer perUserLimit,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        validateName(name);
        validateDiscountType(discountType);
        validateDiscountValue(discountValue);
        validateTotalQuantity(totalQuantity);
        validatePerUserLimit(perUserLimit);
        validateDateRange(startDate, endDate);

        // 정률 할인일 경우 할인율 범위 검증 (0 ~ 100)
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100")) > 0) {
                throw new CouponException(ErrorCode.COUPON_PERCENTAGE_INVALID,
                    "할인율은 0보다 크고 100 이하여야 합니다. 입력값: " + discountValue);
            }
        }

        // 정액 할인일 경우 할인 금액 검증
        if (discountType == DiscountType.FIXED) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(ErrorCode.COUPON_FIXED_AMOUNT_INVALID,
                    "할인 금액은 0보다 커야 합니다. 입력값: " + discountValue);
            }
        }

        // 최소 주문 금액 검증
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_INVALID,
                "최소 주문 금액은 0 이상이어야 합니다. 입력값: " + minOrderAmount);
        }

        // 최대 할인 금액 검증 (정률 할인일 때만)
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_MAX_DISCOUNT_AMOUNT_INVALID,
                "최대 할인 금액은 0보다 커야 합니다. 입력값: " + maxDiscountAmount);
        }

        LocalDateTime now = LocalDateTime.now();

        return new Coupon(
            null,                   // id는 저장 시 생성
            name,
            code,
            discountType,
            discountValue,
            maxDiscountAmount,
            minOrderAmount,
            totalQuantity,
            0,                      // issuedQuantity (초기값 0)
            0,                      // usageCount (초기값 0)
            perUserLimit,
            startDate,
            endDate,
            true,                   // isActive (초기 상태는 활성)
            now,                    // createdAt
            now                     // updatedAt
        );
    }

    // ===== Validation 메서드 =====

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CouponException(ErrorCode.COUPON_NAME_REQUIRED);
        }
    }

    private static void validateDiscountType(DiscountType discountType) {
        if (discountType == null) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_TYPE_REQUIRED);
        }
    }

    private static void validateDiscountValue(BigDecimal discountValue) {
        if (discountValue == null) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_VALUE_REQUIRED);
        }
        if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID);
        }
    }

    private static void validateTotalQuantity(Integer totalQuantity) {
        if (totalQuantity == null || totalQuantity <= 0) {
            throw new CouponException(ErrorCode.COUPON_TOTAL_QUANTITY_INVALID);
        }
    }

    private static void validatePerUserLimit(Integer perUserLimit) {
        if (perUserLimit == null || perUserLimit <= 0) {
            throw new CouponException(ErrorCode.COUPON_PER_USER_LIMIT_INVALID);
        }
    }

    private static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new CouponException(ErrorCode.COUPON_DATE_REQUIRED);
        }
        if (startDate.isAfter(endDate)) {
            throw new CouponException(ErrorCode.COUPON_INVALID_DATE_RANGE,
                "시작일은 종료일보다 이전이어야 합니다. 시작일: " + startDate + ", 종료일: " + endDate);
        }
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 쿠폰 활성화
     */
    public void activate() {
        if (this.isActive) {
            throw new CouponException(ErrorCode.COUPON_ALREADY_ACTIVE);
        }
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 비활성화
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new CouponException(ErrorCode.COUPON_ALREADY_INACTIVE);
        }
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰명 수정
     */
    public void updateName(String name) {
        validateName(name);
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 코드 수정
     */
    public void updateCode(String code) {
        this.code = code;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 할인 정보 수정
     */
    public void updateDiscountInfo(DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount) {
        validateDiscountType(discountType);
        validateDiscountValue(discountValue);

        // 정률 할인일 경우 할인율 범위 검증 (0 ~ 100)
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100")) > 0) {
                throw new CouponException(ErrorCode.COUPON_PERCENTAGE_INVALID,
                    "할인율은 0보다 크고 100 이하여야 합니다. 입력값: " + discountValue);
            }
        }

        // 정액 할인일 경우 할인 금액 검증
        if (discountType == DiscountType.FIXED) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(ErrorCode.COUPON_FIXED_AMOUNT_INVALID,
                    "할인 금액은 0보다 커야 합니다. 입력값: " + discountValue);
            }
        }

        // 최대 할인 금액 검증
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_MAX_DISCOUNT_AMOUNT_INVALID,
                "최대 할인 금액은 0보다 커야 합니다. 입력값: " + maxDiscountAmount);
        }

        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 최소 주문 금액 수정
     */
    public void updateMinOrderAmount(BigDecimal minOrderAmount) {
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_INVALID,
                "최소 주문 금액은 0 이상이어야 합니다. 입력값: " + minOrderAmount);
        }
        this.minOrderAmount = minOrderAmount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 총 수량 수정
     */
    public void updateTotalQuantity(int totalQuantity) {
        validateTotalQuantity(totalQuantity);

        // 이미 발급된 수량보다 작게 수정할 수 없음
        if (totalQuantity < this.issuedQuantity) {
            throw new CouponException(ErrorCode.COUPON_TOTAL_QUANTITY_INVALID,
                "총 수량은 이미 발급된 수량(" + this.issuedQuantity + ")보다 작을 수 없습니다. 입력값: " + totalQuantity);
        }

        this.totalQuantity = totalQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용자당 제한 수정
     */
    public void updatePerUserLimit(int perUserLimit) {
        validatePerUserLimit(perUserLimit);
        this.perUserLimit = perUserLimit;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 사용 기간 수정
     */
    public void updateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 현재 시점 기준 쿠폰 유효성 검사 (편의 메서드)
     */
    public boolean isAvailableNow() {
        return isAvailableNow(LocalDateTime.now());
    }

    /**
     * 특정 시점 기준 쿠폰 유효성 검사
     * @param now 검사 기준 시점
     * @return 쿠폰 사용 가능 여부
     */
    public boolean isAvailableNow(LocalDateTime now) {
        if (!this.isActive) {
            return false;
        }

        if (this.startDate != null && this.startDate.isAfter(now)) {
            return false;  // 아직 시작 안 됨
        }

        if (this.endDate != null && this.endDate.isBefore(now)) {
            return false;  // 이미 만료됨
        }

        return true;
    }

    /**
     * 쿠폰 발급 시 수량 증가
     */
    public void increaseIssuedQuantity() {
        if (this.issuedQuantity >= this.totalQuantity) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED);
        }
        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 시 사용량 증가
     * totalQuantity 제한도 함께 검증
     */
    public void increaseUsageCount() {
        if (this.usageCount >= this.totalQuantity) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED,
                "쿠폰 사용 가능 횟수를 초과했습니다. (총 " + this.totalQuantity + "번 사용 가능)");
        }
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 취소 시 사용량 감소 (보상 트랜잭션용)
     */
    public void decreaseUsageCount() {
        if (this.usageCount <= 0) {
            throw new CouponException(ErrorCode.COUPON_CANNOT_DECREASE_USAGE);
        }
        this.usageCount--;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 발급 가능 여부 (수량 기준)
     */
    public boolean hasRemainingQuantity() {
        return this.issuedQuantity < this.totalQuantity;
    }

    /**
     * 현재 시점 기준 쿠폰 유효성 검증 (편의 메서드)
     */
    public void validateAvailability() {
        validateAvailability(LocalDateTime.now());
    }

    /**
     * 특정 시점 기준 쿠폰 유효성 검증 (예외 발생)
     * @param now 검사 기준 시점
     * @throws CouponException 쿠폰 사용 불가 시
     */
    public void validateAvailability(LocalDateTime now) {
        if (!this.isActive) {
            throw new CouponException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (this.startDate != null && this.startDate.isAfter(now)) {
            throw new CouponException(ErrorCode.COUPON_NOT_STARTED);
        }

        if (this.endDate != null && this.endDate.isBefore(now)) {
            throw new CouponException(ErrorCode.COUPON_EXPIRED);
        }
    }

    /**
     * 주문 금액에 대한 할인 금액 계산
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscountAmount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 최소 주문 금액 검증
        if (this.minOrderAmount != null && orderAmount.compareTo(this.minOrderAmount) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET,
                "최소 주문 금액을 충족하지 못했습니다. 최소 주문 금액: " + this.minOrderAmount + ", 현재 주문 금액: " + orderAmount);
        }

        BigDecimal discountAmount;

        if (this.discountType == DiscountType.PERCENTAGE) {
            // 정률 할인: 주문 금액 * (할인율 / 100)
            discountAmount = orderAmount.multiply(this.discountValue).divide(new BigDecimal("100"), 0, BigDecimal.ROUND_DOWN);

            // 최대 할인 금액 제한 적용
            if (this.maxDiscountAmount != null && discountAmount.compareTo(this.maxDiscountAmount) > 0) {
                discountAmount = this.maxDiscountAmount;
            }
        } else if (this.discountType == DiscountType.FIXED) {
            // 정액 할인: 고정 금액
            discountAmount = this.discountValue;

            // 주문 금액보다 할인 금액이 클 수 없음
            if (discountAmount.compareTo(orderAmount) > 0) {
                discountAmount = orderAmount;
            }
        } else {
            throw new CouponException(ErrorCode.COUPON_INVALID_DISCOUNT_TYPE,
                "지원하지 않는 할인 타입입니다: " + this.discountType);
        }

        return discountAmount;
    }

    // ===== 테스트를 위한 ID 설정 메서드 (인메모리 DB용) =====
    public void setId(Long id) {
        this.id = id;
    }
}
