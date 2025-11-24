package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.UpdateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCouponUseCase {

    private final CouponDomainService couponDomainService;
    private final CouponFinderService couponFinderService;

    @Transactional
    public Coupon execute(UpdateCouponCommand command) {

        // 1. 쿠폰 ID 검증
        couponDomainService.validateId(command.id());

        // 2. 쿠폰 조회
        Coupon coupon = couponFinderService.getCoupon(command.id());

        // 3. 쿠폰 정보 수정
        coupon.updateName(command.name());
        coupon.updateCode(command.code());
        coupon.updateDiscountInfo(
                command.discountType(),
                command.discountValue(),
                command.maxDiscountAmount()
        );
        coupon.updateMinOrderAmount(command.minOrderAmount());
        coupon.updateTotalQuantity(command.totalQuantity());
        coupon.updatePerUserLimit(command.perUserLimit());
        coupon.updateDateRange(command.startDate(), command.endDate());

        // 4. 수정 후 반환
        return coupon;
    }
}