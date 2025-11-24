package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCouponListUseCase {

    private final CouponFinderService couponFinderService;

    @Transactional(readOnly = true)
    public List<Coupon> execute() {
        return couponFinderService.getAllCoupons();
    }
}