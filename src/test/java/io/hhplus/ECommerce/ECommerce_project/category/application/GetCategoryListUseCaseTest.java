package io.hhplus.ECommerce.ECommerce_project.category.application;

import io.hhplus.ECommerce.ECommerce_project.category.domain.entity.Category;
import io.hhplus.ECommerce.ECommerce_project.category.domain.repository.CategoryMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCategoryListUseCaseTest {

    @Mock
    private CategoryMemoryRepository categoryRepository;

    private GetCategoryListUseCase getCategoryListUseCase;

    @BeforeEach
    void setUp() {
        getCategoryListUseCase = new GetCategoryListUseCase(categoryRepository);
    }

    @Test
    void execute_returnsCategoryList() {
        // given
        Category cat1 = Category.createCategory("전자제품", 1);
        cat1.setId(1L);
        Category cat2 = Category.createCategory("생활용품", 2);
        cat2.setId(2L);

        List<Category> categories = List.of(cat1, cat2);
        when(categoryRepository.findAll()).thenReturn(categories);

        // when
        List<Category> result = getCategoryListUseCase.execute();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(categories);
        verify(categoryRepository).findAll();
    }

    @Test
    void execute_returnsEmptyList() {
        // given
        when(categoryRepository.findAll()).thenReturn(List.of());

        // when
        List<Category> result = getCategoryListUseCase.execute();

        // then
        assertThat(result).isEmpty();
        verify(categoryRepository).findAll();
    }
}
