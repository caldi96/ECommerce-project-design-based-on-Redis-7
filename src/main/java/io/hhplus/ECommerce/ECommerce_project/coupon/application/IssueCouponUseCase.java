package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    /**
     * 선착순 쿠폰 발급 (동시성 제어 + 성능 최적화)
     * - 사용자당 1개만 발급
     * - Double-Check Locking 패턴으로 중복 발급 방지
     * - 쿠폰 ID별 비관적 락을 통한 선착순 수량 제어
     * - DB 유니크 제약으로 최종 안전장치
     */
    @Transactional
    public UserCoupon execute(IssueCouponCommand command) {
        // 1. 유저 존재 유무 확인 및 조회
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 빠른 중복 체크 (락 없이) - 이미 발급받은 경우 빠르게 실패
        userCouponRepository.findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                .ifPresent(uc -> {
                    throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
                });

        // 3. 쿠폰 조회 (비관적 락으로 선착순 보장)
        Coupon coupon = couponRepository.findByIdWithLock(command.couponId())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

        // 4. 쿠폰 유효성 검증 (활성화 상태, 사용 기간)
        coupon.validateAvailability();

        // 5. 중복 체크 다시 (Double-Check Locking)
        userCouponRepository.findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                .ifPresent(uc -> {
                    throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
                });

        // 6. 쿠폰 발급 수량 증가 (수량 검증 포함)
        coupon.increaseIssuedQuantity();

        // 7. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issueCoupon(user, coupon);

        try {
            return userCouponRepository.save(userCoupon);
        } catch (DataIntegrityViolationException e) {
            // DB 유니크 제약 위반 = 이미 발급받음 (예상치 못한 케이스)
            throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}