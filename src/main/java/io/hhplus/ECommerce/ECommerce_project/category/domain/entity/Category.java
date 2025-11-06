package io.hhplus.ECommerce.ECommerce_project.category.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    private Long id;
    private String categoryName;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // 논리적 삭제용

    // ===== 정적 팩토리 메서드 =====

    /**
     * 카테고리 생성
     */
    public static Category createCategory(
            String categoryName,
            int displayOrder
    ) {
        validateName(categoryName);
        validateDisplayOrder(displayOrder);

        LocalDateTime now = LocalDateTime.now();

        return new Category(
                null,
                categoryName,
                displayOrder,
                now,
                now,
                null  // deletedAt (삭제되지 않음)
        );
    }

    /**
     * 카테고리명 수정
     */
    public void updateCategoryName(String name) {
        validateName(name);
        this.categoryName = name;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 표시 순서 변경
     * 주의: displayOrder 중복 검증은 Service에서 수행해야 함
     */
    public void updateDisplayOrder(int displayOrder) {
        validateDisplayOrder(displayOrder);

        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 카테고리 삭제 (논리적 삭제)
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_DELETED);
        }

        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ===== Validation 메서드 =====

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CategoryException(ErrorCode.CATEGORY_NAME_REQUIRED);
        }
    }

    private static void validateDisplayOrder(int displayOrder) {
        if (displayOrder <= 0) {
            throw new CategoryException(ErrorCode.DISPLAY_ORDER_INVALID);
        }
    }
}
