package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.UpdateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductCacheInvalidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final CategoryFinderService categoryFinderService;
    private final ProductCacheInvalidator cacheInvalidator;

    @Transactional
    public Product execute(UpdateProductCommand command) {

        // 1. ID 검증
        productDomainService.validateId(command.id());

        // 2. 기존 상품 조회
        Product product = productFinderService.getActiveProduct(command.id());
        Long oldCategoryId = product.getCategory().getId();

        // 3. 도메인 메서드를 통해 각 필드 업데이트
        if (command.name() != null) {
            product.updateName(command.name());
        }

        if (command.description() != null) {
            product.updateDescription(command.description());
        }

        if (command.price() != null) {
            product.updatePrice(command.price());
        }

        if (command.categoryId() != null) {
            Category category = categoryFinderService.getActiveCategory(command.categoryId());
            product.updateCategory(category);
        }

        if (command.minOrderQuantity() != null) {
            product.updateMinOrderQuantity(command.minOrderQuantity());
        }

        if (command.maxOrderQuantity() != null) {
            product.updateMaxOrderQuantity(command.maxOrderQuantity());
        }

        // isActive는 boolean이므로 null 체크 불필요
        if (command.isActive()) {
            if (!product.isActive()) {
                product.activate();
            }
        } else {
            if (product.isActive()) {
                product.deactivate();
            }
        }

        // 4. 캐시 무효화
        Long newCategoryId = product.getCategory().getId();

        // 4-1. 상품 캐시 무효화 (인기상품 캐시)
        cacheInvalidator.evictProductCache(command.id());

        // 4-2. 상품 목록 캐시 무효화
        if (!oldCategoryId.equals(newCategoryId)) {
            // 카테고리가 변경된 경우 양쪽 모두 무효화
            cacheInvalidator.evictProductListCache(oldCategoryId);
            cacheInvalidator.evictProductListCache(newCategoryId);
        } else {
            // 같은 카테고리면 해당 카테고리만 무효화
            cacheInvalidator.evictProductListCache(oldCategoryId);
        }

        // 5. 저장된 변경사항 반환
        return product;
    }

}
