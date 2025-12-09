package io.hhplus.ECommerce.ECommerce_project.coupon.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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
    private final RedisScript<Long> redisCouponIssueScript;

    private static final String COUPON_ISSUE_PREFIX = "coupon:issue:";
    private static final long DEFAULT_TTL_DAYS = 30;
    private static final long DEFAULT_TTL_SECONDS = DEFAULT_TTL_DAYS * 24 * 60 * 60; // 30일

    /**
     * 선착순 쿠폰 발급 시도
     *
     * 장점:
     * 1. 완벽한 원자성 보장 (모든 연산이 하나의 트랜잭션)
     * 2. 네트워크 왕복 최소화 (3 RTT → 1 RTT)
     * 3. Race Condition 완전 방지
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @param maxQuantity 최대 발급 수량
     * @return 발급 성공 여부
     */
    public boolean tryIssueCoupon(Long couponId, Long userId, int maxQuantity) {
        String key = COUPON_ISSUE_PREFIX + couponId;
        long timestamp = System.currentTimeMillis();

        // Lua Script 실행
        Long result = redisTemplate.execute(
                redisCouponIssueScript,
                List.of(key),   // KEYS
                userId.toString(),
                String.valueOf(timestamp),
                String.valueOf(maxQuantity),
                String.valueOf(DEFAULT_TTL_SECONDS) // ARGV
        );

        if (result == null) {
            log.error("Lua Script 실행 실패. couponId={}, userId={}", couponId, userId);
            return false;
        }

        // 결과 처리
        if (result == -1) {
            log.debug("쿠폰 발급 실패: 중복 발급. couponId={}, userId={}", couponId, userId);
            return false;
        } else if (result == -2) {
            log.debug("쿠폰 발급 실패: 수량 초과. couponId={}, userId={}", couponId, userId);
            return false;
        } else if (result >= 0) {
            log.info("쿠폰 발급 성공. couponId={}, userId={}, rank={}", couponId, userId, result + 1);
            return true;
        }

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
