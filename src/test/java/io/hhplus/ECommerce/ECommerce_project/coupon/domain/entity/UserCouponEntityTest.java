package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserCouponEntityTest {

    @Test
    void issueCoupon_success() {
        Long userId = 1L;
        Long couponId = 100L;

        UserCoupon userCoupon = UserCoupon.issueCoupon(userId, couponId);

        assertThat(userCoupon.getUserId()).isEqualTo(userId);
        assertThat(userCoupon.getCouponId()).isEqualTo(couponId);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getUsedCount()).isZero();
        assertThat(userCoupon.getIssuedAt()).isNotNull();
        assertThat(userCoupon.getUsedAt()).isNull();
        assertThat(userCoupon.getExpiredAt()).isNull();
    }

    @Test
    void canUse_and_validateCanUse_success() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);

        assertThat(userCoupon.canUse(2)).isTrue();
        // validateCanUse 예외 없음
        userCoupon.validateCanUse(2);
    }

    @Test
    void useCoupon_success() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        int perUserLimit = 2;

        userCoupon.use(perUserLimit);

        assertThat(userCoupon.getUsedCount()).isEqualTo(1);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getUsedAt()).isNotNull();
    }

    @Test
    void useCoupon_reachesLimit_changesStatusToUsed() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        int perUserLimit = 1;

        userCoupon.use(perUserLimit);

        assertThat(userCoupon.getUsedCount()).isEqualTo(1);
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @Test
    void useCoupon_exceedsLimit_throwsException() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        int perUserLimit = 1;

        // 1회 사용 -> 상태가 USED로 변경
        userCoupon.use(perUserLimit);

        // 2회 사용 시 예외 발생 확인
        assertThatThrownBy(() -> userCoupon.use(perUserLimit))
                .isInstanceOf(CouponException.class)
                .hasMessage("이미 사용된 쿠폰입니다.");
    }

    @Test
    void cancelUse_success() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        int perUserLimit = 2;

        userCoupon.use(perUserLimit);  // usedCount = 1
        userCoupon.cancelUse(perUserLimit);

        assertThat(userCoupon.getUsedCount()).isZero();
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    }

    @Test
    void cancelUse_whenNoUsage_throwsException() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        int perUserLimit = 2;

        assertThatThrownBy(() -> userCoupon.cancelUse(perUserLimit))
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.USER_COUPON_NO_USAGE_TO_CANCEL.getMessage());
    }

    @Test
    void expireCoupon_success() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);

        userCoupon.expire();

        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
        assertThat(userCoupon.getExpiredAt()).isNotNull();
    }

    @Test
    void expireCoupon_alreadyUsed_throwsException() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        userCoupon.use(1); // 상태 = USED

        assertThatThrownBy(userCoupon::expire)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.USER_COUPON_ALREADY_USED.getMessage());
    }

    @Test
    void expireCoupon_alreadyExpired_throwsException() {
        UserCoupon userCoupon = UserCoupon.issueCoupon(1L, 100L);
        userCoupon.expire(); // 상태 = EXPIRED

        assertThatThrownBy(userCoupon::expire)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_EXPIRED.getMessage());
    }
}
