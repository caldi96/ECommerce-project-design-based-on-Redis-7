package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.application.command.CreateCategoryCommand;
import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CategoryException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateCategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CreateCategoryUseCase createCategoryUseCase;

    @BeforeEach
    void setUp() {
        createCategoryUseCase = new CreateCategoryUseCase(categoryRepository);
    }

    @Test
    void execute_success() {
        // given
        CreateCategoryCommand command = new CreateCategoryCommand("전자제품", 1);

        when(categoryRepository.existsByCategoryName("전자제품")).thenReturn(false);
        when(categoryRepository.existsByDisplayOrder(1)).thenReturn(false);

        Category savedCategory = Category.createCategory("전자제품", 1);
        savedCategory.setId(100L);

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // when
        Category result = createCategoryUseCase.execute(command);

        // then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getCategoryName()).isEqualTo("전자제품");
        assertThat(result.getDisplayOrder()).isEqualTo(1);
        assertThat(result.getDeletedAt()).isNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void execute_duplicateName_throwsException() {
        // given
        CreateCategoryCommand command = new CreateCategoryCommand("전자제품", 1);
        when(categoryRepository.existsByCategoryName("전자제품")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> createCategoryUseCase.execute(command))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.CATEGORY_NAME_DUPLICATED.getMessage());

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void execute_duplicateDisplayOrder_throwsException() {
        // given
        CreateCategoryCommand command = new CreateCategoryCommand("전자제품", 1);
        when(categoryRepository.existsByCategoryName("전자제품")).thenReturn(false);
        when(categoryRepository.existsByDisplayOrder(1)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> createCategoryUseCase.execute(command))
                .isInstanceOf(CategoryException.class)
                .hasMessage(ErrorCode.DISPLAY_ORDER_DUPLICATED.getMessage());

        verify(categoryRepository, never()).save(any());
    }
}
