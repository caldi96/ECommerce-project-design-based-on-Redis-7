package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private DeleteCategoryUseCase deleteCategoryUseCase;

    @BeforeEach
    void setUp() {
        deleteCategoryUseCase = new DeleteCategoryUseCase(categoryRepository);
    }

    @Test
    void execute_success() {
        // given
        Long categoryId = 1L;
        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // when
        deleteCategoryUseCase.execute(categoryId);

        // then
        assertThat(category.isDeleted()).isTrue();
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(category);
    }

    @Test
    void execute_categoryNotFound_throwsException() {
        // given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteCategoryUseCase.execute(categoryId))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void execute_alreadyDeleted_throwsException() {
        // given
        Long categoryId = 1L;
        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);
        category.delete(); // 이미 삭제된 상태

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // when & then
        assertThatThrownBy(() -> deleteCategoryUseCase.execute(categoryId))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_ALREADY_DELETED.getMessage());

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }
}
