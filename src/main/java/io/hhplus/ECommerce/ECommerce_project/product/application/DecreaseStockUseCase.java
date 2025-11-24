package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.command.DecreaseStockCommand;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;

    @Transactional
    public Product execute(DecreaseStockCommand command) {

        // 1. ID 조회
        productDomainService.validateId(command.productId());

        // 2. 상품 조회
        Product product = productFinderService.getProductWithLock(command.productId());

        // 3. 재고 감소 (도메인 메서드 활용)
        product.decreaseStock(command.quantity());

        // 4. 저장된 변경사항 반환
        return product;

    }
}