package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons",
        indexes = {
                @Index(name = "idx_user_coupon", columnList = "user_id, coupon_id"),
                @Index(name = "idx_user_status", columnList = "user_id, status"),
        },
        uniqueConstraints = {   // 선착순 쿠폰이 한 사람에게 2장 이상 중복 발급되지 않도록 유니크 제약 조건 걺
                @UniqueConstraint(
                        name = "uk_user_coupon",           // 제약 조건 이름
                        columnNames = {"user_id", "coupon_id"}  // user_id와 coupon_id 조합이 유니크
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCoupon extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;    // ACTIVE, USED, EXPIRED

    @Column(name = "used_count", nullable = false)
    private int usedCount;              // 현재 유저가 사용한 횟수

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 쿠폰 발급
     */
    public static UserCoupon issueCoupon(User user, Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();

        return new UserCoupon(
            null,                           // version (JPA 관리)
            coupon,
            user,
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
