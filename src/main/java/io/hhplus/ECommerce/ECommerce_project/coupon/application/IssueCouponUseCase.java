package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 선착순 쿠폰 발급 (동시성 제어)
     * - 사용자당 1개만 발급
     * - 쿠폰 ID별 락을 통한 Race Condition 방지
     */
    @Transactional
    public UserCoupon execute(IssueCouponCommand command) {
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(command.couponId())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

        // 2. 쿠폰 유효성 검증 (활성화 상태, 사용 기간)
        coupon.validateAvailability();

        // 3. 이미 발급받았는지 확인 (사용자당 1개 제한)
        userCouponRepository.findByUserIdAndCouponId(command.userId(), command.couponId())
                .ifPresent(uc -> {
                    throw new CouponException(ErrorCode.COUPON_USAGE_LIMIT_EXCEEDED,
                        "이미 발급받은 쿠폰입니다.");
                });

        // 4. 쿠폰 발급 가능 수량 확인
        if (!coupon.hasRemainingQuantity()) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED,
                "쿠폰이 모두 소진되었습니다.");
        }

        // 5. 쿠폰 발급 수량 증가 (쿠폰 ID별 락을 통한 Race Condition 방지)
        Coupon updatedCoupon = couponRepository.increaseIssuedQuantityWithLock(command.couponId());

        // 6. 수량 증가 실패 시 예외 처리
        if (updatedCoupon == null) {
            throw new CouponException(ErrorCode.COUPON_NOT_FOUND);
        }

        // 7. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issueCoupon(command.userId(), command.couponId());
        return userCouponRepository.save(userCoupon);
    }
}