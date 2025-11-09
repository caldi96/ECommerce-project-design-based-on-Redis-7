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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ActivateProductUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ActivateProductUseCase useCase = new ActivateProductUseCase(productRepository);

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

        // createProduct() 는 isActive = true 로 생성되므로
        if (!active) {
            product.deactivate(); // 비활성 상태로 맞춤
        }

        product.setId(1L); // 인메모리 환경: ID 수동 설정
        return product;
    }

    @Nested
    @DisplayName("상품 활성화 성공 테스트")
    class ActivateSuccess {

        @Test
        @DisplayName("비활성 상태인 상품을 활성화한다")
        void activateInactiveProduct() {
            // given
            Product product = createProduct(false); // isActive = false
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Product result = useCase.execute(1L);

            // then
            assertThat(result.isActive()).isTrue();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("이미 활성 상태여도 멱등성 보장: 추가 로직 없이 save 호출")
        void activateAlreadyActiveProduct() {
            // given
            Product product = createProduct(true); // 이미 active = true
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Product result = useCase.execute(1L);

            // then
            assertThat(result.isActive()).isTrue(); // 원래부터 true
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("상품 활성화 실패 테스트")
    class ActivateFail {

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
