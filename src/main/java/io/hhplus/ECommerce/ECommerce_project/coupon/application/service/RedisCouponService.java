package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis Sorted Set을 사용한 선착순 쿠폰 발급 서비스
 *
 * 자료구조: Sorted Set
 * - Key: coupon:issue:{couponId}
 * - Score: 발급 시간 (timestamp in milliseconds)
 * - Member: userId
 *
 * 주요 기능:
 * 1. 중복 발급 방지: Sorted Set의 member 유일성 활용
 * 2. 발급 순서 추적: score로 발급 시간 저장
 * 3. 발급 수량 확인: ZCARD로 총 발급 수 조회
 * 4. 원자적 연산: Redis 단일 명령으로 원자성 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCouponService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String COUPON_ISSUE_PREFIX = "coupon:issue:";

    /**
     * 선착순 쿠폰 발급 시도
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @param maxQuantity 최대 발급 수량
     * @return 발급 성공 여부
     */
    public boolean tryIssueCoupon(Long couponId, Long userId, int maxQuantity) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        long timestamp = System.currentTimeMillis();

        // 1. 현재 발급 수량 확인
        Long currentCount = redisTemplate.opsForZSet().zCard(key);
        if (currentCount != null && currentCount >= maxQuantity) {
            log.debug("쿠폰 발급 실패: 수량 초과. couponId={}, currentCount={}, maxQuantity={}",
                    couponId, currentCount, maxQuantity);
            return false;
        }

        // 2. Sorted Set에 추가 (중복 시 false 반환)
        // ZADD NX 옵션: 멤버가 없을 때만 추가
        Boolean added = redisTemplate.opsForZSet().addIfAbsent(key, userId.toString(), timestamp);

        // 3. 이미 발급받은 경우 쿠폰 발급 실패
        if (Boolean.FALSE.equals(added)) {
            log.debug("쿠폰 발급 실패: 중복 발급. couponId={}, userId={}", couponId, userId);
            return false;
        }

        // 4. 본인의 순위 확인 (0부터 시작, 0 = 1등)
        Long rank = redisTemplate.opsForZSet().rank(key, userId.toString());

        // 5. 순위가 maxQuantity 이내인지 확인
        if (rank != null && rank < maxQuantity) {
            // TTL 설정 (30일 후 자동 삭제)
            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl == -1) {
                redisTemplate.expire(key, Duration.ofDays(30));
            }

            log.info("쿠폰 발급 성공. couponId={}, userId={}, rank={}", couponId, userId, rank + 1);
            return true;
        }

        // 6. 순위 초과 시 제거 후 실패
        redisTemplate.opsForZSet().remove(key, userId.toString());
        log.debug("쿠폰 발급 실패: 수량 초과. couponId={}, userId={}, rank={}",
                couponId, userId, rank != null ? rank + 1 : "unknown");
        return false;
    }

    /**
     * 특정 사용자의 쿠폰 발급 여부 확인
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 여부
     */
    public boolean isAlreadyIssued(Long couponId, Long userId) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        return score != null;
    }

    /**
     * 현재 쿠폰 발급 수량 조회
     *
     * @param couponId 쿠폰 ID
     * @return 발급 수량
     */
    public long getIssuedCount(Long couponId) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0L;
    }

    /**
     * 쿠폰 발급 취소 (환불 등의 경우)
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 취소 성공 여부
     */
    public boolean cancelIssueCoupon(Long couponId, Long userId) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        Long removed = redisTemplate.opsForZSet().remove(key, userId.toString());

        boolean success = removed != null && removed > 0;

        if (success) {
            log.info("쿠폰 발급 취소. couponId={}, userId={}", couponId, userId);
        }

        return success;
    }

    /**
     * 특정 쿠폰의 발급 순위 조회
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 순위 (0부터 시작, 없으면 null)
     */
    public Long getIssuanceRank(Long couponId, Long userId) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        // rank는 0부터 시작 (0 = 1등)
        return redisTemplate.opsForZSet().rank(key, userId.toString());
    }

    /**
     * Redis 쿠폰 발급 데이터 초기화
     *
     * @param couponId 쿠폰 ID
     */
    public void clearCouponIssueData(Long couponId) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        redisTemplate.delete(key);
        log.info("쿠폰 발급 데이터 초기화. couponId={}", couponId);
    }
}
