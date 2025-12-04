package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.annotation.DistributedLock;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.RedisCouponService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponValidatorService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final UserDomainService userDomainService;
    private final CouponDomainService couponDomainService;
    private final UserFinderService userFinderService;
    private final CouponFinderService couponFinderService;
    private final UserCouponValidatorService userCouponValidatorService;
    private final RedisCouponService redisCouponService;

    /**
     * Redis 분산 락 + REQUIRES_NEW 트랜잭션 자동 적용됨
     */
    @DistributedLock(
            key = "'coupon:issue:' + #command.couponId()",
            waitTime = 2L,
            leaseTime = 5L
    )
    public UserCoupon execute(IssueCouponCommand command) {

        userDomainService.validateId(command.userId());
        couponDomainService.validateId(command.couponId());

        // 1. 유저 존재 유무 확인 및 조회
        User user = userFinderService.getUser(command.userId());

        // 2. 빠른 중복 체크 (락 없이) - 이미 발급받은 경우 빠르게 실패
        userCouponValidatorService.checkAlreadyIssued(command.userId(), command.couponId());

        // 3. 쿠폰 조회 (비관적 락 제거 - 분산 락으로 동시성 제어)
        Coupon coupon = couponFinderService.getCoupon(command.couponId());

        // 4. 쿠폰 유효성 검증 (활성화 상태, 사용 기간)
        coupon.validateAvailability();

        // 5. Redis Lua Script로 선착순 발급 시도
        boolean issued = redisCouponService.tryIssueCoupon(
                command.couponId(),
                command.userId(),
                coupon.getTotalQuantity()
        );

        if (!issued) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED);
        }


        // 6. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issueCoupon(user, coupon);

        try {
            // 7. 쿠폰 발급 수량 증가 (수량 검증 포함)
            UserCoupon savedCoupon = userCouponRepository.save(userCoupon);
            coupon.increaseIssuedQuantity();    // DB 수량 동기화
            return savedCoupon;
        } catch (DataIntegrityViolationException e) {
            // DB 저장 실패 시 Redis 롤백
            redisCouponService.cancelIssueCoupon(command.couponId(), command.userId());
            // DB 유니크 제약 위반 = 이미 발급받음 (예상치 못한 케이스)
            throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
        } catch (Exception e) {
            // 기타 예외 발생 시에도 Redis 롤백
            redisCouponService.cancelIssueCoupon(command.couponId(), command.userId());
            throw new CouponException(ErrorCode.COUPON_ISSUE_FAILED);
        }
    }
}