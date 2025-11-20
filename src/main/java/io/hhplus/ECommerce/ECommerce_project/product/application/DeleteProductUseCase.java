package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;

    @Transactional
    public void execute(Long productId) {

        // 1. ID 조회
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getProductWithLock(productId);

        // 3. 논리적 삭제 (deletedAt 설정 및 비활성화)
        product.delete();
    }
}
