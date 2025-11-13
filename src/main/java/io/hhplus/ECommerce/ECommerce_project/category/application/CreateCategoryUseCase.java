package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.CreateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category execute(CreateCategoryCommand command) {

        // 1. 카테고리명 중복 체크
        if (categoryRepository.existsByCategoryNameAndDeletedAtIsNull(command.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }

        // 2. 표시 순서 중복 체크
        if (categoryRepository.existsByDisplayOrderAndDeletedAtIsNull(command.displayOrder())) {
            throw new CategoryException(ErrorCode.DISPLAY_ORDER_DUPLICATED);
        }

        // 3. 도메인 생성
        Category category = Category.createCategory(
                command.name(),
                command.displayOrder()
        );
        // 저장 후 반환
        return categoryRepository.save(category);
    }
}
