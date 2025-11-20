package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.command.UpdatePriceCommand;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePriceUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;

    @Transactional
    public Product execute(UpdatePriceCommand command) {

        // 1. ID 검증
        productDomainService.validateId(command.productId());

        // 2. 상품 조회
        Product product = productFinderService.getActiveProduct(command.productId());

        // 3. 가격 변경
        product.updatePrice(command.price());

        // 4. 저장된 변경사항 반환
        return product;
    }
}