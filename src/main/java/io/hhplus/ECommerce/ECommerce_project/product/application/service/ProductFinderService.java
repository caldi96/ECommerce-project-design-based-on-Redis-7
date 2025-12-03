package io.hhplus.ECommerce.ECommerce_project.product.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFinderService {

    private final ProductRepository productRepository;

    /**
     * 상품 단건 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 삭제되지 않은 상품 조회
     */
    public Product getActiveProduct(Long productId) {
        return productRepository.findByIdActive(productId)
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
     * 전체 상품 조회
     */
    public List<Product> getAllProduct() {
        return productRepository.findAll();
    }

    /**
     * 필터링/정렬/페이징된 상품 조회
     */
    public Page<Product> getProductPage(Long categoryId, Pageable pageable) {
        return productRepository.findProducts(categoryId, pageable);
    }

    /**
     * 인기상품 조회
     */
    public List<Product> getTop20Products(Pageable pageable) {
        return productRepository.findTop20Products(pageable);
    }

    /**
     * 상품 ID 값으로 모든 상품 가져오기
     */
    public List<Product> getAllProductsById(List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }
}
