package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserCouponFinderService {

    private final UserCouponRepository userCouponRepository;

    /**
     * 유저 ID와 쿠폰 ID로 유저 쿠폰 조회
     */
    public Optional<UserCoupon> getUserCouponByUserIdAndCouponId(Long userId, Long couponId) {
        return userCouponRepository.findByUser_IdAndCoupon_Id(userId, couponId);
    }

    /**
     * UserCoupon 조회 (비관적 락)
     */
    public UserCoupon getUserCouponWithLock(User user, Coupon coupon) {
        return userCouponRepository
                .findByUser_IdAndCoupon_IdWithLock(user.getId(), coupon.getId())
                .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));
    }
}
