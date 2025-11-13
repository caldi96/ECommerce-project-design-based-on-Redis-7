package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateCouponUseCase {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon execute(Long id) {
        // 1. 마스터 쿠폰 조회
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

        // 2. 쿠폰 비활성화
        coupon.deactivate();

        // 3.저장 후 반환
        return coupon;
    }
}
