package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;

    @Transactional
    public Product execute(Long productId) {

        // 1. ID 검증
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getActiveProduct(productId);

        // 3. 조회수 증가
        product.increaseViewCount();

        // 4. 저장된 변경사항 반환
        return product;
    }
}