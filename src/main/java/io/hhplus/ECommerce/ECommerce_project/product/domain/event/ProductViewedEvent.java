package io.hhplus.ECommerce.ECommerce_project.product.domain.event;

/**
 * 상품 조회 이벤트
 * - 상품 조회 시 발행
 * - Redis 랭킹 업데이트를 위한 비동기 이벤트
 */
public record ProductViewedEvent(
        Long productId
) {
    public static ProductViewedEvent of(Long productId) {
        return new ProductViewedEvent(productId);
    }
}
