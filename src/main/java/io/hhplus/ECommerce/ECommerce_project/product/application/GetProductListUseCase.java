package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.service.CategoryDomainService;
import io.hhplus.ECommerce.ECommerce_project.product.application.dto.ProductPageResult;
import io.hhplus.ECommerce.ECommerce_project.product.application.enums.ProductSortType;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final CategoryDomainService categoryDomainService;
    private final ProductFinderService productFinderService;

    @Transactional(readOnly = true)
    public ProductPageResult execute(Long categoryId, ProductSortType sortType, int page, int size) {

        // 1. ID 검증
        categoryDomainService.validateId(categoryId);

        // 2. ProductSortType을 Spring Data Sort로 변환
        Sort sort = convertToSort(sortType);

        // 3. Pageable 생성
        Pageable pageable = PageRequest.of(page, size, sort);

        // 4. Repository에서 필터링/정렬/페이징된 상품 조회
        Page<Product> productPage = productFinderService.getProductPage(categoryId, pageable);

        // 5. 결과 반환 (Page 객체의 모든 정보 활용)
        return new ProductPageResult(
            productPage.getContent(),
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.isFirst(),
            productPage.isLast()
        );
    }

    /**
     * ProductSortType을 Spring Data Sort로 변환
     */
    private Sort convertToSort(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case POPULAR -> Sort.by(Sort.Direction.DESC, "soldCount");
            case VIEWED -> Sort.by(Sort.Direction.DESC, "viewCount");
            case PRICE_LOW -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_HIGH -> Sort.by(Sort.Direction.DESC, "price");
        };
    }
}
