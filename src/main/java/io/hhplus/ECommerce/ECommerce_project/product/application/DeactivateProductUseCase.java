package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisRankingService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductCacheInvalidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeactivateProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final ProductCacheInvalidator cacheInvalidator;
    private final RedisRankingService redisRankingService;

    @Transactional
    public Product execute(Long productId) {

        // 1. ID 검증
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getActiveProduct(productId);
        Long categoryId = product.getCategory().getId();

        // 3. 비활성화 (이미 비활성화되어 있어도 멱등성 보장)
        if (product.isActive()) {
            product.deactivate();
        }

        // 4. Redis 랭킹에서 제거 (비활성 상품은 랭킹에 표시 안 함)
        redisRankingService.removeFromRanking(productId);

        // 5. 전체 캐시 무효화 (상품 캐시 + 목록 캐시)
        cacheInvalidator.clearProductCaches(productId, categoryId);

        // 6. 저장된 변경사항 반환
        return product;
    }
}