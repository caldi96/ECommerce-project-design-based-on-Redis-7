package io.hhplus.ECommerce.ECommerce_project.category.application.service;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.infrastructure.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryValidatorService {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리명 중복 체크
     */
    public void validateNameNotDuplicated(String name) {
        if (categoryRepository.existsByCategoryNameAndDeletedAtIsNull(name)) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }

    /**
     * 자신을 제외한 다른 id와의 카테고리명 중복 체크
     */
    public void validateNameNotDuplicatedExceptSelf(String name, Long id) {
        if (categoryRepository.existsByCategoryNameAndIdNotAndDeletedAtIsNull(name, id)) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }

    /**
     * 표시 순서 중복 체크
     */
    public void validateDisplayOrderNotDuplicated(int displayOrder) {
        if (categoryRepository.existsByDisplayOrderAndDeletedAtIsNull(displayOrder)) {
            throw new CategoryException(ErrorCode.CATEGORY_DISPLAY_ORDER_DUPLICATED);
        }
    }

    /**
     * 자신을 제외한 다른 id와의 표시 순서 중복 체크
     */
    public void validateDisplayOrderNotDuplicatedExceptSelf(int displayOrder, Long id) {
        if (categoryRepository.existsByDisplayOrderAndIdNotAndDeletedAtIsNull(displayOrder, id)) {
            throw new CategoryException(ErrorCode.CATEGORY_DISPLAY_ORDER_DUPLICATED);
        }
    }
}
