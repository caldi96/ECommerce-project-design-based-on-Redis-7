package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.CreateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Product execute(CreateProductCommand command) {
        // 1. 도메인 생성
        Product product = Product.createProduct(
                categoryRepository.findByIdAndDeletedAtIsNull(command.categoryId())
                                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND)),
                command.name(),
                command.description(),
                command.price(),
                command.stock(),
                command.minOrderQuantity(),
                command.maxOrderQuantity()
        );

        // 2. 저장 후 반환
        return productRepository.save(product);
    }
}
