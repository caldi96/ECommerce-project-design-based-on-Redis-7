package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.service.CategoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.CreateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductCacheInvalidator;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryFinderService categoryFinderService;
    private final ProductCacheInvalidator productCacheInvalidator;

    @Transactional
    public Product execute(CreateProductCommand command) {
        // 1. 도메인 생성
        Product product = Product.createProduct(
                categoryFinderService.getActiveCategory(command.categoryId()),
                command.name(),
                command.description(),
                command.price(),
                command.stock(),
                command.minOrderQuantity(),
                command.maxOrderQuantity()
        );

        // 2. 저장 후 반환
        Product savedProduct = productRepository.save(product);

        // 3. 캐시 무효화 (해당 카테고리만)
        productCacheInvalidator.evictProductListCache(command.categoryId());

        return savedProduct;
    }
}
