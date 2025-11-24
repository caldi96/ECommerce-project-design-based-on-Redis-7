package io.hhplus.ECommerce.ECommerce_project.category.application.service;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryFinderService {

    private final CategoryRepository categoryRepository;

    /**
     * 활성 상태 카테고리 조회
     */
    public Category getActiveCategory(Long id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /**
     * 활성 상태 카테고리 전체 조회
     */
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findAllByDeletedAtIsNull();
    }
}
