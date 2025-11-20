package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductFinderService {

    private final ProductRepository productRepository;

    /**
     * 삭제되지 않은 상품 조회
     */
    public Product getActiveProduct(Long id) {
        return productRepository.findByIdActive(id)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 상품 조회 (비관적 락)
     */
    public Product getProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 필터링/정렬/페이징된 상품 조회
     */
    public Page<Product> getProductPage(Long categoryId, Pageable pageable) {
        return productRepository.findProducts(categoryId, pageable);
    }
}
