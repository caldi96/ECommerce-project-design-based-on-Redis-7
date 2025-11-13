package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.UpdateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Product execute(UpdateProductCommand command) {
        // 1. 기존 상품 조회
        Product product = productRepository.findByIdActive(command.id())
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 도메인 메서드를 통해 각 필드 업데이트
        if (command.name() != null) {
            product.updateName(command.name());
        }

        if (command.description() != null) {
            product.updateDescription(command.description());
        }

        if (command.price() != null) {
            product.updatePrice(command.price());
        }

        if (command.categoryId() != null) {
//            product.updateCategoryId(command.categoryId());
            Category category = categoryRepository.findByIdAndDeletedAtIsNull(command.categoryId())
                    .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));
            product.updateCategory(category);
        }

        if (command.minOrderQuantity() != null) {
            product.updateMinOrderQuantity(command.minOrderQuantity());
        }

        if (command.maxOrderQuantity() != null) {
            product.updateMaxOrderQuantity(command.maxOrderQuantity());
        }

        // isActive는 boolean이므로 null 체크 불필요
        if (command.isActive()) {
            if (!product.isActive()) {
                product.activate();
            }
        } else {
            if (product.isActive()) {
                product.deactivate();
            }
        }

        // 3. 저장된 변경사항 반환
        return product;
    }

}
