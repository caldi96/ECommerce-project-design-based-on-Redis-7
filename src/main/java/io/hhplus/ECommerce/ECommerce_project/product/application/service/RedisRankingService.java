package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRankingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DAILY_RANKING_PREFIX = "ranking:daily:";
    private static final String WEEKLY_RANKING_PREFIX = "ranking:weekly:";

    /**
     * 일별 인기상품 Sort Set 초기화
     * @param productId
     * @param soldCount
     * @param viewCount
     */
    public void initializeDailyProductScore(Long productId, int soldCount, int viewCount) {
        double score = calculateScore(soldCount, viewCount);
        LocalDate today = LocalDate.now();  // YYYY-MM-DD

        // 일별 랭킹 업데이트
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        redisTemplate.opsForZSet().add(dailyKey, productId.toString(), score);

        // dailyKey TTL 설정 (7일 후 자동 삭제)
        redisTemplate.expire(dailyKey, Duration.ofDays(7));
    }

    public void initializeWeeklyProductScore(Long productId, int soldCount, int viewCount) {
        double score = calculateScore(soldCount, viewCount);
        LocalDate today = LocalDate.now();  // YYYY-MM-DD
        WeekFields weekFields = WeekFields.ISO;

        // 해당 주의 랭킹 업데이트
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());
        redisTemplate.opsForZSet().add(weeklyKey, productId.toString(), score);

        // weeklyKey TTL 설정 (4주 후 자동 삭제)
        redisTemplate.expire(weeklyKey, Duration.ofDays(28));
    }

    /**
     * 점수 계산 (판매량 우선)
     * @param soldCount
     * @param viewCount
     * @return
     */
    private double calculateScore(int soldCount, int viewCount) {
        return soldCount * 10000.0 + viewCount;
    }

    /**
     * 오늘의 인기상품 TOP N 조회
     */
    public List<Long> getTodayTopProducts(int limit) {
        LocalDate today = LocalDate.now();
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");

        // ZREVRANGE: 점수 높은 순으로 조회
        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(dailyKey, 0, limit - 1);

        if (result == null) {
            return List.of();
        }

        return result.stream()
                .map(Long::parseLong)
                .toList();
    }

    /**
     * 주간 인기상품 TOP N 조회
     * 현재 주의 주간 랭킹 조회
     */
    public List<Long> getWeeklyTopProducts(int limit) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // ZREVRANGE: 점수 높은 순으로 조회
        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(weeklyKey, 0, limit - 1);

        if (result == null) {
            return List.of();
        }

        return result.stream()
                .map(Long::parseLong)
                .toList();
    }

    /**
     * 판매 발생 시 score 증가 (ZINCBY 활용)
     */
    public void incrementSoldCount(Long productId, int quantity) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // 판매량 * 10000 만큼 점수 증가
        double incrementScore = calculateScore(quantity, 0);

        // ZINCRBY는 멤버가 없으면 자동으로 추가하므로 체크 불필요
        incrementDailyScore(dailyKey, productId, incrementScore);
        incrementWeeklyScore(weeklyKey, productId, incrementScore);
    }

    /**
     * 조회 발생 시 score 증가
     */
    public void incrementViewCount(Long productId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // ZINCRBY는 멤버가 없으면 자동으로 추가하므로 체크 불필요
        incrementDailyScore(dailyKey, productId, 1.0);
        incrementWeeklyScore(weeklyKey, productId, 1.0);
    }

    /**
     * 환불 발생 시 score 감소 (ZINCBY 활용 - 음수를 이용해 감소)
     */
    public void decrementSoldCount(Long productId, int quantity, LocalDateTime paidAt) {
        LocalDate paidDay = paidAt.toLocalDate();
        WeekFields weekFields = WeekFields.ISO;
        String dailyKey = DAILY_RANKING_PREFIX + paidDay.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + paidDay.getYear() + "-W" + paidDay.get(weekFields.weekOfYear());

        // 판매량 * 10000 만큼 점수 감소
        double decrementScore = calculateScore(quantity, 0) * (-1);

        // 일별 및 주간 점수 감소
        incrementDailyScore(dailyKey, productId, decrementScore);
        incrementWeeklyScore(weeklyKey, productId, decrementScore);
    }

    /**
     * redis Zset에 일별 점수 증가
     */
    public void incrementDailyScore(String dailyKey, Long productId, Double score) {
        redisTemplate.opsForZSet().incrementScore(
                dailyKey,
                productId.toString(),
                score
        );

        // TTL이 없으면 설정 (이미 있으면 유지)
        Long ttl = redisTemplate.getExpire(dailyKey);
        // ZINCRBY는 멤버가 없으면 자동으로 추가하지만 TTL을 따로 설정해줘야 한다.
        // dailyKey TTL 설정 (7일 후 자동 삭제)
        if (ttl == null || ttl == -1) { // -1은 TTL이 설정되지 않음을 의미
            redisTemplate.expire(dailyKey, Duration.ofDays(7));
        }
    }

    /**
     * redis Zset에 주간 점수 증가
     */
    public void incrementWeeklyScore(String weeklyKey, Long productId, Double score) {
        redisTemplate.opsForZSet().incrementScore(
                weeklyKey,
                productId.toString(),
                score
        );

        // TTL이 없으면 설정 (이미 있으면 유지)
        Long ttl = redisTemplate.getExpire(weeklyKey);
        // ZINCRBY는 멤버가 없으면 자동으로 추가하지만 TTL을 따로 설정해줘야 한다.
        // weeklyKey TTL 설정 (4주 후 자동 삭제)
        if (ttl == null || ttl == -1) { // -1은 TTL이 설정되지 않음을 의미
            redisTemplate.expire(weeklyKey, Duration.ofDays(28));
        }
    }

    /**
     * 특정 멤버(상품)이 ZSET 안에 존재하는
     */
    public boolean existsInDailyOfZSet(Long productId) {
        LocalDate today = LocalDate.now();
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");

        Double score = redisTemplate.opsForZSet().score(dailyKey, productId.toString());

        return score != null;
    }

    public boolean existsInWeeklyOfZSet(Long productId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        Double score = redisTemplate.opsForZSet().score(weeklyKey, productId.toString());

        return score != null;
    }

    /**
     * 상품을 모든 랭킹에서 제거 (상품 비활성화/삭제 시)
     * - 현재 날짜/주차의 랭킹에서만 제거
     * - 과거 랭킹은 TTL로 자동 삭제
     */
    public void removeFromRanking(Long productId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;

        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // 일별/주간 랭킹에서 제거
        Long dailyRemoved = redisTemplate.opsForZSet().remove(dailyKey, productId.toString());
        Long weeklyRemoved = redisTemplate.opsForZSet().remove(weeklyKey, productId.toString());

        log.info("상품을 랭킹에서 제거 - productId: {}, daily: {}, weekly: {}",
                productId, dailyRemoved > 0 ? "제거됨" : "없음", weeklyRemoved > 0 ? "제거됨" : "없음");
    }
}
