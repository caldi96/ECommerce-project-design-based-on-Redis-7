package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeactivateProductUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final DeactivateProductUseCase useCase = new DeactivateProductUseCase(productRepository);

    /**
     * 테스트용 상품 생성 (기본값: 활성 상태)
     */
    private Product createProduct(boolean active) {
        Product product = Product.createProduct(
                "상품A",
                10L,
                "설명",
                BigDecimal.valueOf(1000),
                10,
                null,
                null
        );

        if (!active) {
            product.deactivate(); // 초기 값을 비활성으로 맞춤
        }

        product.setId(1L);
        return product;
    }

    @Nested
    @DisplayName("상품 비활성화 성공 테스트")
    class DeactivateSuccess {

        @Test
        @DisplayName("활성 상태인 상품을 비활성화한다")
        void deactivateActiveProduct() {
            // given
            Product product = createProduct(true); // active = true
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Product result = useCase.execute(1L);

            // then
            assertThat(result.isActive()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("이미 비활성 상태여도 멱등성 보장 (예외 없음)")
        void deactivateAlreadyInactiveProduct() {
            // given
            Product product = createProduct(false); // 이미 active = false
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Product result = useCase.execute(1L);

            // then
            assertThat(result.isActive()).isFalse(); // 그대로 false
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("상품 비활성화 실패 테스트")
    class DeactivateFail {

        @Test
        @DisplayName("상품이 존재하지 않으면 예외 발생")
        void productNotFound() {
            // given
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.execute(1L))
                    .isInstanceOf(ProductException.class)
                    .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

            verify(productRepository, never()).save(any());
        }
    }
}
