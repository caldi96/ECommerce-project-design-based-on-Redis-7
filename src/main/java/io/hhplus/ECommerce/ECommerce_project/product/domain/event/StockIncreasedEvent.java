package io.hhplus.ECommerce.ECommerce_project.product.domain.event;

/**
 * 재고 증가 이벤트
 * - 재고 복구(보상 트랜잭션) 시 발행
 * - DB 동기화를 위한 비동기 이벤트
 */
public record StockIncreasedEvent(
        Long productId,
        Integer quantity
) {
}