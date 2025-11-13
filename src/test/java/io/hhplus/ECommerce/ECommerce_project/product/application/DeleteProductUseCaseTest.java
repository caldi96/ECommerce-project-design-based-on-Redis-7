package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeleteProductUseCaseTest {

    private ProductMemoryRepository productRepository;
    private DeleteProductUseCase useCase;

    @BeforeEach
    void setup() {
        productRepository = mock(ProductMemoryRepository.class);
        useCase = new DeleteProductUseCase(productRepository);
    }

    private Product createProduct(Long id) {
        Product product = Product.createProduct(
                "테스트상품",
                1L,
                "설명",
                new BigDecimal("1000"),
                10,
                1,
                5
        );
        product.setId(id); // ★ 엔티티에 직접 id 넣는 테스트용 setter 필요 (테스트 전용)
        return product;
    }

    @Test
    @DisplayName("상품 삭제 성공 - 논리삭제 플래그 설정 + 비활성화")
    void deleteSuccess() {
        // given
        Product product = createProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        useCase.execute(1L);

        // then
        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.isActive()).isFalse();

        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("상품 삭제 실패 - 상품 없음")
    void deleteFail_notFound() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(ProductException.class)
                .satisfies(ex ->
                        assertThat(((ProductException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
                );

        verify(productRepository, never()).save(any());
    }
}
