package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductCacheInvalidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final ProductCacheInvalidator productCacheInvalidator;

    @Transactional
    public void execute(Long productId) {

        // 1. ID 조회
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getProductWithLock(productId);
        Long categoryId = product.getCategory().getId();    // 카테고리 ID 저장

        // 3. 논리적 삭제 (deletedAt 설정 및 비활성화)
        product.delete();

        // 캐시 무효화
        productCacheInvalidator.evictProductListCache(categoryId);
    }
}
