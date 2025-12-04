package io.hhplus.ECommerce.ECommerce_project.product.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheInvalidator {

    private static final int FIRST_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DAILY_CACHE_PREFIX = "ranking:product:cache:daily:";
    private static final String WEEKLY_CACHE_PREFIX = "ranking:product:cache:weekly:";

    private final CacheManager redisCacheManager;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 상품 목록 캐시 무효화 (특정 카테고리)
     */
    public void evictProductListCache(Long categoryId) {
        Cache cache = redisCacheManager.getCache("productList");
        if (cache != null) {
            // 5가지 정렬 × 첫페이지 캐시 삭제
            for (ProductSortType sortType : ProductSortType.values()) {
                String key = categoryId + "_" + sortType + "_" + FIRST_PAGE + "_" + DEFAULT_PAGE_SIZE;
                cache.evict(key);
            }
            log.debug("상품 목록 캐시 무효화 완료 - categoryId: {}", categoryId);
        }
    }

    /**
     * 상품 캐시 무효화 (일별/주간 인기상품 캐시)
     * - 현재 날짜/주차의 캐시만 삭제 (과거 데이터는 TTL로 자동 삭제)
     */
    public void evictProductCache(Long productId) {
        try {
            LocalDate today = LocalDate.now();
            WeekFields weekFields = WeekFields.ISO;

            // 일별 캐시 삭제
            String dailyKey = DAILY_CACHE_PREFIX + today.toString().replace("-", "") + ":" + productId;
            redisTemplate.delete(dailyKey);

            // 주간 캐시 삭제
            String weeklyKey = WEEKLY_CACHE_PREFIX + today.getYear() + "-W" + today.get(weekFields.weekOfYear()) + ":" + productId;
            redisTemplate.delete(weeklyKey);

            log.debug("상품 캐시 무효화 완료 - productId: {}, dailyKey: {}, weeklyKey: {}",
                     productId, dailyKey, weeklyKey);
        } catch (Exception e) {
            log.error("상품 캐시 무효화 실패 - productId: {}", productId, e);
        }
    }

    /**
     * 상품 관련 모든 캐시 무효화 (상품 삭제 시)
     * - 상품 목록 캐시 + 인기상품 캐시
     */
    public void clearProductCaches(Long productId, Long categoryId) {
        evictProductCache(productId);
        evictProductListCache(categoryId);
        log.info("상품 전체 캐시 무효화 완료 - productId: {}, categoryId: {}", productId, categoryId);
    }

    /**
     * 상품 목록 캐시 전체 무효화 (모든 카테고리)
     */
    public void evictAllProductListCache() {
        Cache cache = redisCacheManager.getCache("productList");
        if (cache != null) {
            cache.clear();
            log.info("상품 목록 캐시 전체 무효화 완료");
        }
    }
}
