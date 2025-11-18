package io.hhplus.ECommerce.ECommerce_project.category.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Category extends BaseEntity {

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
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

        return new Category(
                categoryName,
                displayOrder,
                null,   // createdAt (JPA @CreationTimestamp가 자동 설정)
                null,   // updatedAt (JPA @UpdateTimestamp가 자동 설정)
                null    // deletedAt (삭제되지 않음)
        );
    }

    /**
     * 카테고리명 수정
     */
    public void updateCategoryName(String name) {
        validateName(name);
        this.categoryName = name;
    }

    /**
     * 표시 순서 변경
     * 주의: displayOrder 중복 검증은 Service에서 수행해야 함
     */
    public void updateDisplayOrder(int displayOrder) {
        validateDisplayOrder(displayOrder);

        this.displayOrder = displayOrder;
    }

    /**
     * 카테고리 삭제 (논리적 삭제)
     */
    public void delete() {
        if (this.deletedAt != null) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_DELETED);
        }

        this.deletedAt = LocalDateTime.now();
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
            throw new CategoryException(ErrorCode.CATEGORY_DISPLAY_ORDER_INVALID);
        }
    }
}
