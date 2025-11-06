package io.hhplus.ECommerce.ECommerce_project.category.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getCategoryName(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public static List<CategoryResponse> from(List<Category> categoryList) {
        return categoryList.stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
