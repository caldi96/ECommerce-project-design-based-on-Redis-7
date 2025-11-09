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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private GetCategoryUseCase getCategoryUseCase;

    @BeforeEach
    void setUp() {
        getCategoryUseCase = new GetCategoryUseCase(categoryRepository);
    }

    @Test
    void execute_success() {
        // given
        Long categoryId = 1L;
        Category category = Category.createCategory("전자제품", 1);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // when
        Category result = getCategoryUseCase.execute(categoryId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getCategoryName()).isEqualTo("전자제품");
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void execute_categoryNotFound_throwsException() {
        // given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getCategoryUseCase.execute(categoryId))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());

        verify(categoryRepository).findById(categoryId);
    }
}
