package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {

    private Long id;
    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "coupon_id")
    // private Coupon coupon;
    private Long couponId;
    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id")
    // private User user;
    private Long userId;
    private UserCouponStatus status;    // ACTIVE, USED, EXPIRED
    private int usedCount;              // 현재 유저가 사용한 횟수
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime issuedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 쿠폰 발급
     */
    public static UserCoupon issueCoupon(Long userId, Long couponId) {
        LocalDateTime now = LocalDateTime.now();

        return new UserCoupon(
            null,                           // id는 저장 시 생성
            couponId,
            userId,
            UserCouponStatus.AVAILABLE,     // 발급 시 사용 가능 상태
            0,                              // usedCount (초기값 0)
            null,                           // usedAt (아직 사용 안 함)
            null,                           // expiredAt (만료 안 됨)
            now                             // issuedAt (발급 시점)
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public boolean canUse(int perUserLimit) {
        return this.status == UserCouponStatus.AVAILABLE && this.usedCount < perUserLimit;
    }

    /**
     * 쿠폰 사용 가능 여부 검증 (예외 발생)
     */
    public void validateCanUse(int perUserLimit) {
        if (this.status == UserCouponStatus.USED) {
            throw new CouponException(ErrorCode.USER_COUPON_ALREADY_USED);
        }

        if (this.status == UserCouponStatus.EXPIRED) {
            throw new CouponException(ErrorCode.COUPON_EXPIRED);
        }

        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new CouponException(ErrorCode.USER_COUPON_NOT_AVAILABLE);
        }

        if (this.usedCount >= perUserLimit) {
            throw new CouponException(ErrorCode.COUPON_USAGE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 쿠폰 사용 처리
     */
    public void use(int perUserLimit) {
        validateCanUse(perUserLimit);

        this.usedCount++;
        this.usedAt = LocalDateTime.now();

        // 사용 횟수 제한에 도달하면 USED 상태로 변경
        if (this.usedCount >= perUserLimit) {
            this.status = UserCouponStatus.USED;
        }
    }

    /**
     * 쿠폰 사용 취소 (보상 트랜잭션용)
     */
    public void cancelUse(int perUserLimit) {
        if (this.usedCount <= 0) {
            throw new CouponException(ErrorCode.USER_COUPON_NO_USAGE_TO_CANCEL);
        }

        this.usedCount--;

        // USED 상태였는데 사용 횟수가 제한 미만으로 줄어들면 AVAILABLE로 복구
        if (this.status == UserCouponStatus.USED && this.usedCount < perUserLimit) {
            this.status = UserCouponStatus.AVAILABLE;
        }
    }

    /**
     * 쿠폰 만료 처리
     */
    public void expire() {
        if (this.status == UserCouponStatus.USED) {
            throw new CouponException(ErrorCode.USER_COUPON_ALREADY_USED);
        }

        if (this.status == UserCouponStatus.EXPIRED) {
            throw new CouponException(ErrorCode.COUPON_EXPIRED);
        }

        this.status = UserCouponStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }
}
