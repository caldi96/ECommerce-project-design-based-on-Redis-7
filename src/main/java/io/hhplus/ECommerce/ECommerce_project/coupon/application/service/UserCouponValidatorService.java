package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCouponValidatorService {

    private final UserCouponFinderService userCouponFinderService;

    /**
     * 유저 쿠폰 발급 시 중복 체크
     */
    public void checkAlreadyIssued(Long userId, Long couponId) {
        userCouponFinderService.getUserCouponByUserIdAndCouponId(userId, couponId)
                .ifPresent(uc -> {
                    throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
                });
    }
}
