package io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class CouponMemoryRepository implements CouponRepository {
    private final Map<Long, Coupon> couponMap = new ConcurrentHashMap<>();
    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 쿠폰 ID별 락 객체 획득
     */
    private Object getLock(Long couponId) {
        return lockMap.computeIfAbsent(couponId, k -> new Object());
    }

    @Override
    public Coupon save(Coupon coupon) {
        // ID가 없으면 Snowflake ID 생성
        if (coupon.getId() == null) {
            coupon.setId(idGenerator.nextId());
        }
        couponMap.put(coupon.getId(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return Optional.ofNullable(couponMap.get(id));
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponMap.values().stream()
                .filter(coupon -> coupon.getCode().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public List<Coupon> findAll() {
        return new ArrayList<>(couponMap.values());
    }

    @Override
    public List<Coupon> findAllByIsActiveTrueAndStartDateBeforeAndEndDateAfter(LocalDateTime now1, LocalDateTime now2) {
        return couponMap.values().stream()
                .filter(Coupon::isActive)
                .filter(coupon -> coupon.getStartDate().isBefore(now1) && coupon.getEndDate().isAfter(now2))
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        couponMap.remove(id);
    }

    /**
     * 동시성 제어를 위한 쿠폰 발급 수량 증가
     * 쿠폰 ID별로 락을 걸어서 Race Condition 방지
     */
    @Override
    public Coupon increaseIssuedQuantityWithLock(Long couponId) {
        Object lock = getLock(couponId);

        synchronized (lock) {
            Coupon coupon = couponMap.get(couponId);
            if (coupon != null) {
                coupon.increaseIssuedQuantity();
                couponMap.put(couponId, coupon);
            }
            return coupon;
        }
    }
}
