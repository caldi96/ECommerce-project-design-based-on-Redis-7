package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.product.domain.event.StockDecreasedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.domain.event.StockIncreasedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final RedisStockService redisStockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 재고 차감 (Redis) - 초고속 처리
     * CreateOrderFromProductUseCase.java 에서 사용
     */
    public void reserveStock(Long productId, Integer quantity) {
        // 1. Redis에서 즉시 재고 차감 (5-10ms)
        Long remaining = redisStockService.decreaseStock(productId, quantity);

        // 2. DB 동기화 이벤트 발행 (비동기)
        applicationEventPublisher.publishEvent(
                new StockDecreasedEvent(productId, quantity)
        );
    }

    /**
     * 재고 복구 (Redis)
     * CreateOrderFromProductUseCase.java 에서 사용
     */
    public void compensateStock(Long productId, Integer quantity) {
        // Redis 재고 복구
        redisStockService.increaseStock(productId, quantity);

        // DB 동기화 이벤트 발행
        applicationEventPublisher.publishEvent(
                new StockIncreasedEvent(productId, quantity)
        );
    }

    /**
     * 여러 상품 재고 차감 (Redis) - 초고속 처리
     * CreateOrderFromCartUseCase.java 에서 사용
     */
    public void reserveStocks(List<Map.Entry<Long, Integer>> sortedEntries) {
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // Redis에서 즉시 재고 차감 (5-10ms)
            Long remaining = redisStockService.decreaseStock(productId, totalQuantity);

            // DB 동기화 이벤트 발행 (비동기)
            applicationEventPublisher.publishEvent(
                    new StockDecreasedEvent(productId, totalQuantity)
            );
        }
    }

    /**
     * 여러 상품 재고 복구 (Redis)
     * CreateOrderFromCartUseCase.java 에서 사용
     */
    public void compensateStocks(List<Map.Entry<Long, Integer>> sortedEntries) {
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // Redis 재고 복구
            redisStockService.increaseStock(productId, totalQuantity);

            // DB 동기화 이벤트 발행
            applicationEventPublisher.publishEvent(
                    new StockIncreasedEvent(productId, totalQuantity)
            );
        }
    }
}
