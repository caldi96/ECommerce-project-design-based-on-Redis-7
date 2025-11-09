package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public void execute(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        // 논리적 삭제
        category.delete();
        categoryRepository.save(category);
    }
}
