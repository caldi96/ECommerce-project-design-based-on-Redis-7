package io.hhplus.ECommerce.ECommerce_project.category.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class CategoryEntityTest {

    @Test
    void createCategory_success() {
        Category category = Category.createCategory("전자제품", 1);

        assertThat(category.getCategoryName()).isEqualTo("전자제품");
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getUpdatedAt()).isNotNull();
        assertThat(category.getDeletedAt()).isNull();
    }

    @Test
    void createCategory_invalidName_throwsException() {
        assertThatThrownBy(() -> Category.createCategory("", 1))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage());

        assertThatThrownBy(() -> Category.createCategory(null, 1))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage());
    }

    @Test
    void createCategory_invalidDisplayOrder_throwsException() {
        assertThatThrownBy(() -> Category.createCategory("전자제품", 0))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_INVALID.getMessage());

        assertThatThrownBy(() -> Category.createCategory("전자제품", -5))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_INVALID.getMessage());
    }

    @Test
    void updateCategoryName_success() {
        Category category = Category.createCategory("전자제품", 1);
        category.updateCategoryName("가전제품");

        assertThat(category.getCategoryName()).isEqualTo("가전제품");
        assertThat(category.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateCategoryName_invalidName_throwsException() {
        Category category = Category.createCategory("전자제품", 1);

        assertThatThrownBy(() -> category.updateCategoryName(""))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage());

        assertThatThrownBy(() -> category.updateCategoryName(null))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage());
    }

    @Test
    void updateDisplayOrder_success() {
        Category category = Category.createCategory("전자제품", 1);
        category.updateDisplayOrder(5);

        assertThat(category.getDisplayOrder()).isEqualTo(5);
        assertThat(category.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateDisplayOrder_invalidOrder_throwsException() {
        Category category = Category.createCategory("전자제품", 1);

        assertThatThrownBy(() -> category.updateDisplayOrder(0))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_INVALID.getMessage());

        assertThatThrownBy(() -> category.updateDisplayOrder(-3))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_INVALID.getMessage());
    }

    @Test
    void delete_success() {
        Category category = Category.createCategory("전자제품", 1);
        category.delete();

        assertThat(category.isDeleted()).isTrue();
        assertThat(category.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_alreadyDeleted_throwsException() {
        Category category = Category.createCategory("전자제품", 1);
        category.delete();

        assertThatThrownBy(category::delete)
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_ALREADY_DELETED.getMessage());
    }
}
