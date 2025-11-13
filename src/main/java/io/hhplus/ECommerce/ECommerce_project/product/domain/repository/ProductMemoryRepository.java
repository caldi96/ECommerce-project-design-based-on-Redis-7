package io.hhplus.ECommerce.ECommerce_project.product.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductMemoryRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    /**
     * 상품 조회 (비관적 락 적용)
     * 동시성 제어가 필요한 경우 사용 (재고 차감 등)
     */
    Optional<Product> findByIdWithLock(Long id);

    /**
     * 재고 차감 (비관적 락 적용)
     * 락 안에서 상품 조회, 검증, 재고 차감, 판매량 증가를 원자적으로 수행
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @return 업데이트된 상품
     */
    Product decreaseStockWithLock(Long productId, int quantity);

    /**
     * 재고 복구 (비관적 락 적용)
     * 락 안에서 재고 증가 및 판매량 감소를 원자적으로 수행
     * @param productId 상품 ID
     * @param quantity 복구할 수량
     * @return 업데이트된 상품
     */
    Product restoreStockWithLock(Long productId, int quantity);

    List<Product> findAll();

    List<Product> findAllById(List<Long> ids);

    void deleteById(Long id);

    /**
     * 활성화된 상품 목록 조회 (페이징, 카테고리 필터링, 정렬)
     */
    List<Product> findProducts(Long categoryId, ProductSortType sortType, int page, int size);

    /**
     * 활성화된 상품 총 개수 (페이징 메타데이터용)
     */
    long countActiveProducts(Long categoryId);
}
