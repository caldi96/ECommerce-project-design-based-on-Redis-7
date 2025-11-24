package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivateProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;

    @Transactional
    public Product execute(Long productId) {

        // 1. ID 조회
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getActiveProduct(productId);

        // 3. 활성화 (이미 활성화되어 있어도 멱등성 보장)
        if (!product.isActive()) {
            product.activate();
        }

        // 4. 저장된 변경사항 반환
        return product;
    }
}