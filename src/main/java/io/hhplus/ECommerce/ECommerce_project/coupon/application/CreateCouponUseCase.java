package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.CreateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon execute(CreateCouponCommand command) {
        // 1. 도메인 생성
        Coupon coupon = Coupon.createCoupon(
                command.name(),
                command.code(),
                command.discountType(),
                command.discountValue(),
                command.maxDiscountAmount(),
                command.minOrderAmount(),
                command.totalQuantity(),
                command.perUserLimit(),
                command.startDate(),
                command.endDate()
        );
        // 저장 후 반환
        return couponRepository.save(coupon);
    }
}
