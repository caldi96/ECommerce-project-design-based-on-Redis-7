package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.UpdateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category execute(UpdateCategoryCommand command) {
        // 1. 카테고리 존재 유무
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(command.id())
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. 카테고리명 중복 체크
        if (categoryRepository.existsByCategoryNameAndIdNotAndDeletedAtIsNull(command.name(), command.id())) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }

        // 3. 표시 순서 중복 체크
        if (categoryRepository.existsByDisplayOrderAndIdNotAndDeletedAtIsNull(command.displayOrder(), command.id())) {
            throw new CategoryException(ErrorCode.DISPLAY_ORDER_DUPLICATED);
        }

        // 4. 도메인 수정
        category.updateCategoryName(command.name());
        category.updateDisplayOrder(command.displayOrder());

        return category;
    }
}
