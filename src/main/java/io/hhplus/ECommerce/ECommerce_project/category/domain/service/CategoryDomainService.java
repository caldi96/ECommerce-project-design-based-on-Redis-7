package io.hhplus.ECommerce.ECommerce_project.category.domain.service;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryDomainService {

    /**
     * 이름이 비어있는지 검증
     */
    public void validateNameNotEmpty(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_REQUIRED);
        }
    }

    /**
     * 표시 순서가 양수인지 검증
     */
    public void validateDisplayOrderPositive(int displayOrder) {
        if (displayOrder <= 0) {
            throw new CategoryException(ErrorCode.CATEGORY_DISPLAY_ORDER_INVALID);
        }
    }

    /**
     * displayOrder 중복 여부 검증 (리스트 기반, DB 접근 없음)
     */
    public void validateDisplayOrderUnique(int displayOrder, List<Category> allCategories) {
        boolean exists = allCategories.stream()
                .anyMatch(c -> c.getDisplayOrder() == displayOrder);
        if (exists) {
            throw new CategoryException(ErrorCode.CATEGORY_DISPLAY_ORDER_DUPLICATED);
        }
    }

    /**
     * 삭제 가능한 상태인지 검증
     */
    public void validateDeletable(Category category) {
        if (category.isDeleted()) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_DELETED);
        }
    }

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new CategoryException(ErrorCode.CATEGORY_ID_INVALID);
        }
    }
}
