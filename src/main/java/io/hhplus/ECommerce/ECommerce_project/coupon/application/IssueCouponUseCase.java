package io.hhplus.ECommerce.ECommerce_project.coupon.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.lock.DistributedLockManager;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final UserDomainService userDomainService;
    private final CouponDomainService couponDomainService;
    private final UserFinderService userFinderService;
    private final CouponFinderService couponFinderService;
    private final UserCouponValidatorService userCouponValidatorService;
    private final DistributedLockManager distributedLockManager;

    /**
     * 선착순 쿠폰 발급 (Redis 분산 락 사용)
     * - 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 종료 → 락 해제
     * - 쿠폰 ID별 락으로 동시성 제어
     */
    public UserCoupon execute(IssueCouponCommand command) {
        String lockKey = "coupon:issue:" + command.couponId();

        // 분산락 획득 후 트랜잭션 실행
        return distributedLockManager.executeWithLock(
                lockKey,
                2L,
                5L,
                TimeUnit.SECONDS,
                () -> executeWithTransaction(command)
        );
    }

    /**
     * 트랜잭션 내에서 실행되는 실제 비즈니스 로직
     * - 분산 락 획득 후 호출됨
     * - REQUIRES_NEW로 새로운 트랜잭션 시작 (명확한 트랜잭션 경계)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserCoupon executeWithTransaction(IssueCouponCommand command) {

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

        // 5. 쿠폰 발급 수량 증가 (수량 검증 포함)
        coupon.increaseIssuedQuantity();

        // 6. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.issueCoupon(user, coupon);

        try {
            return userCouponRepository.save(userCoupon);
        } catch (DataIntegrityViolationException e) {
            // DB 유니크 제약 위반 = 이미 발급받음 (예상치 못한 케이스)
            throw new CouponException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}