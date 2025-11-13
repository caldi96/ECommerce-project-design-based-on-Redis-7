package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.UpdateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryUseCaseTest {

    @Mock
    private CategoryMemoryRepository categoryRepository;

    private UpdateCategoryUseCase updateCategoryUseCase;

    @BeforeEach
    void setUp() {
        updateCategoryUseCase = new UpdateCategoryUseCase(categoryRepository);
    }

    @Test
    void execute_success() {
        // given
        Long categoryId = 1L;
        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, "가전제품", 2);

        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameExceptId("가전제품", categoryId)).thenReturn(false);
        when(categoryRepository.existsByDisplayOrderExceptId(2, categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // when
        Category updatedCategory = updateCategoryUseCase.execute(command);

        // then
        assertThat(updatedCategory.getCategoryName()).isEqualTo("가전제품");
        assertThat(updatedCategory.getDisplayOrder()).isEqualTo(2);
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByCategoryNameExceptId("가전제품", categoryId);
        verify(categoryRepository).existsByDisplayOrderExceptId(2, categoryId);
        verify(categoryRepository).save(category);
    }

    @Test
    void execute_categoryNotFound_throwsException() {
        Long categoryId = 1L;
        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, "가전제품", 2);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateCategoryUseCase.execute(command))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void execute_duplicateName_throwsException() {
        Long categoryId = 1L;
        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, "가전제품", 2);

        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameExceptId("가전제품", categoryId)).thenReturn(true);

        assertThatThrownBy(() -> updateCategoryUseCase.execute(command))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_DUPLICATED.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByCategoryNameExceptId("가전제품", categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void execute_duplicateDisplayOrder_throwsException() {
        Long categoryId = 1L;
        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, "가전제품", 2);

        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCategoryNameExceptId("가전제품", categoryId)).thenReturn(false);
        when(categoryRepository.existsByDisplayOrderExceptId(2, categoryId)).thenReturn(true);

        assertThatThrownBy(() -> updateCategoryUseCase.execute(command))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_DUPLICATED.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByCategoryNameExceptId("가전제품", categoryId);
        verify(categoryRepository).existsByDisplayOrderExceptId(2, categoryId);
        verify(categoryRepository, never()).save(any());
    }
}
