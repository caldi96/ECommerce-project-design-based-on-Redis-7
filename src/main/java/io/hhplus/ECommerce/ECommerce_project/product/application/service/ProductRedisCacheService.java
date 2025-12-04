package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DAILY_CACHE_PREFIX = "ranking:product:cache:daily:";
    private static final String WEEKLY_CACHE_PREFIX = "ranking:product:cache:weekly:";
    private static final Duration DAILY_TTL = Duration.ofDays(7);
    private static final Duration WEEKLY_TTL = Duration.ofDays(28);

    // ===== 일별 캐시 =====

    /**
     * 일별 캐시 키 생성: ranking:product:cache:daily:20251204:{productId}
     */
    private String getDailyCacheKey(Long productId) {
        LocalDate today = LocalDate.now();
        String dateStr = today.toString().replace("-", "");  // YYYYMMDD
        return DAILY_CACHE_PREFIX + dateStr + ":" + productId;
    }

    /**
     * 일별 상품 캐싱
     */
    public void cacheDailyProduct(Product product) {
        try {
            String dailyKey = getDailyCacheKey(product.getId());
            ProductCacheDto dto = ProductCacheDto.from(product);
            String json = objectMapper.writeValueAsString(dto);

            redisTemplate.opsForValue().set(dailyKey, json, DAILY_TTL);

            log.debug("일별 상품 캐시 저장 - key: {}", dailyKey);
        } catch (JsonProcessingException e) {
            log.error("일별 상품 캐시 저장 실패 - productId: {}", product.getId(), e);
        }
    }

    /**
     * 일별 상품 조회
     */
    public Optional<ProductCacheDto> getDailyProduct(Long productId) {
        try {
            String dailyKey = getDailyCacheKey(productId);
            String json = redisTemplate.opsForValue().get(dailyKey);

            if (json == null) {
                log.debug("일별 캐시 미스 - productId: {}", productId);
                return Optional.empty();
            }

            ProductCacheDto dto = objectMapper.readValue(json, ProductCacheDto.class);
            log.debug("일별 캐시 히트 - productId: {}", productId);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.error("일별 캐시 조회 실패 - productId: {}", productId, e);
            return Optional.empty();
        }
    }

    // ===== 주간 캐시 =====

    /**
     * 주간 캐시 키 생성: product:cache:weekly:2025-W48:{productId}
     */
    private String getWeeklyCacheKey(Long productId) {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        String weekStr = today.getYear() + "-W" + today.get(weekFields.weekOfYear());
        return WEEKLY_CACHE_PREFIX + weekStr + ":" + productId;
    }

    /**
     * 주간 상품 캐싱
     */
    public void cacheWeeklyProduct(Product product) {
        try {
            String key = getWeeklyCacheKey(product.getId());
            ProductCacheDto dto = ProductCacheDto.from(product);
            String json = objectMapper.writeValueAsString(dto);

            redisTemplate.opsForValue().set(key, json, WEEKLY_TTL);

            log.debug("주간 상품 캐시 저장 - key: {}", key);
        } catch (JsonProcessingException e) {
            log.error("주간 상품 캐시 저장 실패 - productId: {}", product.getId(), e);
        }
    }

    /**
     * 주간 상품 조회
     */
    public Optional<ProductCacheDto> getWeeklyProduct(Long productId) {
        try {
            String key = getWeeklyCacheKey(productId);
            String json = redisTemplate.opsForValue().get(key);

            if (json == null) {
                log.debug("주간 캐시 미스 - productId: {}", productId);
                return Optional.empty();
            }

            ProductCacheDto dto = objectMapper.readValue(json, ProductCacheDto.class);
            log.debug("주간 캐시 히트 - productId: {}", productId);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.error("주간 캐시 조회 실패 - productId: {}", productId, e);
            return Optional.empty();
        }
    }

    // ===== 캐시 DTO =====

    public record ProductCacheDto(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String description,
            String price,  // BigDecimal → String
            int stock,
            boolean isActive,
            int viewCount,
            int soldCount,
            Integer minOrderQuantity,
            Integer maxOrderQuantity
    ) {
        public static ProductCacheDto from(Product product) {
            return new ProductCacheDto(
                    product.getId(),
                    product.getCategory().getId(),
                    product.getCategory().getCategoryName(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice().toString(),
                    product.getStock(),
                    product.isActive(),
                    product.getViewCount(),
                    product.getSoldCount(),
                    product.getMinOrderQuantity(),
                    product.getMaxOrderQuantity()
            );
        }
    }
}
