package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
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
     * 인기상품 점수 업데이트
     * @param productId
     * @param soldCount
     * @param viewCount
     */
    public void updateProductScore(Long productId, int soldCount, int viewCount) {
        double score = calculateScore(soldCount, viewCount);
        LocalDate today = LocalDate.now();  // YYYY-MM-DD
        WeekFields weekFields = WeekFields.ISO;

        // 일별 랭킹 업데이트
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        redisTemplate.opsForZSet().add(dailyKey, productId.toString(), score);

        // 해당 주의 랭킹 업데이트
        int week; // 주차
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());
        redisTemplate.opsForZSet().add(weeklyKey, productId.toString(), score);

        // dailyKey TTL 설정 (7일 후 자동 삭제)
        redisTemplate.expire(dailyKey, Duration.ofDays(7));

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
        String key = DAILY_RANKING_PREFIX + today.toString().replace("-", "");

        // ZREVRANGE: 점수 높은 순으로 조회
        Set<String> result = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);

        if (result == null) {
            return List.of();
        }

        return result.stream()
                .map(Long::parseLong)
                .toList();
    }

    /**
     * 주간 인기상품 TOP N 조회
     */
    /*
    public List<Long> getWeeklyTopProducts(int limit) {
        // 최근 7일치 데이터
    }

     */

    /**
     * 판매 발생 시 점수 증가 (ZINCBY 활용)
     */
    public void incrementSoldCount(Long productId, int quantity) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // 판매량 * 10000 만큼 점수 증가
        double incrementScore = calculateScore(quantity, 0);

        redisTemplate.opsForZSet().incrementScore(
                dailyKey,
                productId.toString(),
                incrementScore
        );

        redisTemplate.opsForZSet().incrementScore(
                weeklyKey,
                productId.toString(),
                incrementScore
        );
    }

    /**
     * 조회 발생 시 점수 증가
     */
    public void incrementViewCount(Long productId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String dailyKey = DAILY_RANKING_PREFIX + today.toString().replace("-", "");
        String weeklyKey = WEEKLY_RANKING_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear());

        // 조회수 1 증가
        redisTemplate.opsForZSet().incrementScore(
                dailyKey,
                productId.toString(),
                1.0
        );

        redisTemplate.opsForZSet().incrementScore(
                weeklyKey,
                productId.toString(),
                1.0
        );
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
}
