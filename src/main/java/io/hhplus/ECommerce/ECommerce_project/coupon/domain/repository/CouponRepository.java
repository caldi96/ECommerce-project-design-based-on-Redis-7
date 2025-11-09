package io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAll();

    /**
     * 현재 시점 기준으로 활성화된 쿠폰 조회
     * (isActive = true AND startDate <= now AND endDate >= now)
     */
    List<Coupon> findAllByIsActiveTrueAndStartDateBeforeAndEndDateAfter(LocalDateTime now1, LocalDateTime now2);

    void deleteById(Long id);

    /**
     * 동시성 제어를 위한 쿠폰 발급 수량 증가 (인메모리 전용)
     * @param couponId 쿠폰 ID
     * @return 업데이트된 쿠폰
     */
    Coupon increaseIssuedQuantityWithLock(Long couponId);

    /**
     * 동시성 제어를 위한 쿠폰 조회 (비관적 락)
     * @param couponId 쿠폰 ID
     * @return 쿠폰 Optional
     */
    Optional<Coupon> findByIdWithLock(Long couponId);
}
