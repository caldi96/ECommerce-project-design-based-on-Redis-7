package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.event.ProductViewedEvent;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Product execute(Long productId) {

        // 1. ID 검증
        productDomainService.validateId(productId);

        // 2. 상품 조회
        Product product = productFinderService.getActiveProduct(productId);

        // 3. 조회수 증가
        product.increaseViewCount();

        // 4. 상품 조회 이벤트 발행 (Redis 조회수는 비동기로 처리)
        applicationEventPublisher.publishEvent(ProductViewedEvent.of(productId));

        // 5. 저장된 변경사항 반환
        return product;
    }
}