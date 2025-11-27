package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponCompensationService {

    private final UserCouponFinderService userCouponFinderService;

    /**
     * 주문 취소/결제 실패 시 쿠폰 복구
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void compensate(Long userId, Long couponId, Integer perUserLimit) {
        UserCoupon userCoupon = userCouponFinderService
                .getUserCouponByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

        userCoupon.cancelUse(perUserLimit);
    }
}
