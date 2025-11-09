package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCouponUseCase {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public Coupon execute(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));
    }
}
