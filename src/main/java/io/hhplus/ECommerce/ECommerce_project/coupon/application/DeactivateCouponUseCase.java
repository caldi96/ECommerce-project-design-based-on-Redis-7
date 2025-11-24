package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateCouponUseCase {

    private final CouponDomainService couponDomainService;
    private final CouponFinderService couponFinderService;

    @Transactional
    public Coupon execute(Long id) {

        // 1. 쿠폰 ID 검증
        couponDomainService.validateId(id);

        // 2. 마스터 쿠폰 조회
        Coupon coupon = couponFinderService.getCoupon(id);

        // 3. 쿠폰 비활성화
        coupon.deactivate();

        // 4.저장 후 반환
        return coupon;
    }
}
