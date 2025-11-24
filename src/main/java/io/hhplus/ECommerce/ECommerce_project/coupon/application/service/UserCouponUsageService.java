package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCouponUsageService {

    private final UserCouponRepository userCouponRepository;
    private final UserCouponFinderService userCouponFinderService;
    private final CouponFinderService couponFinderService;

    /**
     * 쿠폰 사용 처리
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return Coupon 엔티티 (Order 생성에 필요)
     */
    @Transactional
    public Coupon useCoupon(Long userId, Long couponId) {
        // 1. 사용자 쿠폰 조회
        UserCoupon userCoupon = userCouponFinderService
                .getUserCouponByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

        // 2. 쿠폰 조회 및 검증
        Coupon coupon = couponFinderService.getCoupon(couponId);

        // 3. 쿠폰 유효성 검증
        coupon.validateAvailability();

        // 4. 사용자 쿠폰 사용 가능 여부 확인
        userCoupon.validateCanUse(coupon.getPerUserLimit());

        // 5. 쿠폰 사용 처리
        userCoupon.use(coupon.getPerUserLimit());
        userCouponRepository.save(userCoupon); // 테스트 코드 dirty check 오류때문에 추가

        return coupon;
    }
}