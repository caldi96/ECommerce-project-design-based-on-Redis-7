package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.UpdateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCouponUseCase {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon execute(UpdateCouponCommand command) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(command.id())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

        // 2. 쿠폰 정보 수정
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

        // 3. 저장 후 반환
        return coupon;
    }
}