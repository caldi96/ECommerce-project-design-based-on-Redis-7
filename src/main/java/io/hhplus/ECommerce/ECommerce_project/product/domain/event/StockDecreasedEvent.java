package io.hhplus.ECommerce.ECommerce_project.product.domain.event;

/**
 * 재고 차감 이벤트
 * - Redis에서 재고 차감 후 발행
 * - DB 동기화를 위한 비동기 이벤트
 */
public record StockDecreasedEvent(
        Long productId,
        Integer quantity
) {
}