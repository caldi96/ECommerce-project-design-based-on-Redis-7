package io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // 코드로 쿠폰 조회
    Optional<Coupon> findByCodeIgnoreCase(String code);

    // 사용가능한 쿠폰 목록 조회

    @Query("""
            SELECT c
            FROM Coupon c
            WHERE c.isActive = true
              AND c.startDate <= CURRENT_TIMESTAMP
              AND c.endDate >= CURRENT_TIMESTAMP
            """)
    List<Coupon> findAllAvailableCoupons();

    // 비관적 락(PESSIMISTIC_WRITE) 으로 쿠폰 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId")
    Optional<Coupon> findByIdWithLock(@Param("couponId") Long couponId);
}
