package io.hhplus.ECommerce.ECommerce_project.coupon.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;

import java.time.LocalDateTime;
import java.util.List;

public record UserCouponResponse(
        Long id,
        Long couponId,
        Long userId,
        UserCouponStatus status,
        int usedCount,
        LocalDateTime usedAt,
        LocalDateTime expiredAt,
        LocalDateTime issuedAt
) {
    public static UserCouponResponse from(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCouponId(),
                userCoupon.getUserId(),
                userCoupon.getStatus(),
                userCoupon.getUsedCount(),
                userCoupon.getUsedAt(),
                userCoupon.getExpiredAt(),
                userCoupon.getIssuedAt()
        );
    }

    public static List<UserCouponResponse> from(List<UserCoupon> userCouponList) {
        return userCouponList.stream()
                .map(UserCouponResponse::from)
                .toList();
    }
}