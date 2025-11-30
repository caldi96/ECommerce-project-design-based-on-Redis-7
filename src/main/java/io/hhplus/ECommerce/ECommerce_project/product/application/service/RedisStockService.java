package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 기반 재고 관리 서비스
 * - Lua Script를 사용하여 원자적 재고 차감 보장
 * - 초당 1000건 이상 고트래픽 처리 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock:product:";

    /**
     * Lua Script: 원자적 재고 차감
     * - Redis의 단일 명령으로 실행되어 원자성 보장
     * - 락 없이도 동시성 안전
     */
    private static final String DECREASE_STOCK_SCRIPT =
            """
            local key = KEYS[1]
            local quantity = tonumber(ARGV[1])
            local current = tonumber(redis.call('GET', key) or '0')

            if current >= quantity then
                redis.call('DECRBY', key, quantity)
                return current - quantity
            else
                return -1
            end
            """;

    /**
     * 재고 차감 (원자적)
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @return 차감 후 남은 재고
     * @throws ProductException 재고 부족 시
     */
    public Long decreaseStock(Long productId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            // Lua Script 실행 (원자적 보장, 락 불필요!)
            Long remaining = redisTemplate.execute(
                    RedisScript.of(DECREASE_STOCK_SCRIPT, Long.class),
                    List.of(key),
                    quantity.toString()
            );

            if (remaining == null || remaining == -1) {
                log.warn("재고 부족: productId={}, 요청수량={}", productId, quantity);
                throw new ProductException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            log.debug("재고 차감 성공: productId={}, 차감수량={}, 남은재고={}",
                    productId, quantity, remaining);

            return remaining;

        } catch (ProductException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis 재고 차감 실패: productId={}", productId, e);
            throw new ProductException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 재고 증가 (보상 트랜잭션용)
     *
     * @param productId 상품 ID
     * @param quantity 증가할 수량
     * @return 증가 후 재고
     */
    public Long increaseStock(Long productId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            Long newStock = redisTemplate.opsForValue().increment(key, quantity);

            log.debug("재고 증가 성공: productId={}, 증가수량={}, 현재재고={}",
                    productId, quantity, newStock);

            return newStock;

        } catch (Exception e) {
            log.error("Redis 재고 증가 실패: productId={}", productId, e);
            throw new ProductException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 현재 재고 조회
     *
     * @param productId 상품 ID
     * @return 현재 재고 (없으면 0)
     */
    public Long getStock(Long productId) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;

        } catch (Exception e) {
            log.error("Redis 재고 조회 실패: productId={}", productId, e);
            return 0L;
        }
    }

    /**
     * 재고 설정 (초기화용)
     *
     * @param productId 상품 ID
     * @param stock 설정할 재고
     */
    public void setStock(Long productId, Integer stock) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            redisTemplate.opsForValue().set(key, stock.toString());
            log.debug("재고 설정 완료: productId={}, stock={}", productId, stock);

        } catch (Exception e) {
            log.error("Redis 재고 설정 실패: productId={}", productId, e);
            throw new ProductException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 재고 삭제 (상품 삭제 시)
     *
     * @param productId 상품 ID
     */
    public void deleteStock(Long productId) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            redisTemplate.delete(key);
            log.debug("재고 삭제 완료: productId={}", productId);

        } catch (Exception e) {
            log.error("Redis 재고 삭제 실패: productId={}", productId, e);
        }
    }

    /**
     * 재고 존재 여부 확인
     *
     * @param productId 상품 ID
     * @return 존재 여부
     */
    public boolean existsStock(Long productId) {
        String key = STOCK_KEY_PREFIX + productId;

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));

        } catch (Exception e) {
            log.error("Redis 재고 존재 확인 실패: productId={}", productId, e);
            return false;
        }
    }
}