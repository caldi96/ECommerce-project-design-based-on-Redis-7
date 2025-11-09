package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.DecreaseStockCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DecreaseStockUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final DecreaseStockUseCase useCase = new DecreaseStockUseCase(productRepository);

    /**
     * 테스트용 상품 생성
     */
    private Product createProduct(int stock) {
        Product product = Product.createProduct(
                "상품A",
                10L,
                "설명",
                BigDecimal.valueOf(1000),
                stock,
                null,
                null
        );
        product.setId(1L);
        return product;
    }

    @Nested
    @DisplayName("재고 감소 성공 테스트")
    class DecreaseSuccess {

        @Test
        @DisplayName("정상적으로 재고 감소")
        void decreaseStockSuccess() {
            // given
            Product product = createProduct(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            DecreaseStockCommand command = new DecreaseStockCommand(1L, 3);

            // when
            Product result = useCase.execute(command);

            // then
            assertThat(result.getStock()).isEqualTo(7);
            assertThat(result.isSoldOutProduct()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("재고를 차감한 결과 0이 되면 품절 처리됨")
        void decreaseStockToZero() {
            // given
            Product product = createProduct(3); // 재고 3
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            DecreaseStockCommand command = new DecreaseStockCommand(1L, 3);

            // when
            Product result = useCase.execute(command);

            // then
            assertThat(result.getStock()).isEqualTo(0);
            assertThat(result.isSoldOutProduct()).isTrue(); // 품절 상태
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("재고 감소 실패 테스트")
    class DecreaseFail {

        @Test
        @DisplayName("상품이 존재하지 않으면 예외")
        void productNotFound() {
            // given
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            DecreaseStockCommand command = new DecreaseStockCommand(1L, 5);

            // when & then
            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(ProductException.class)
                    .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("차감 수량이 0 이하이면 예외")
        void invalidDecreaseQuantity() {
            // given
            Product product = createProduct(10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            DecreaseStockCommand command = new DecreaseStockCommand(1L, 0);

            // when & then
            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(ProductException.class)
                    .hasMessage(ErrorCode.PRODUCT_DECREASE_QUANTITY_INVALID.getMessage());
        }

        @Test
        @DisplayName("재고보다 큰 수량을 요청하면 예외")
        void insufficientStock() {
            // given
            Product product = createProduct(5); // 재고 5
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            DecreaseStockCommand command = new DecreaseStockCommand(1L, 10);

            // when & then
            assertThatThrownBy(() -> useCase.execute(command))
                    .isInstanceOf(ProductException.class)
                    .satisfies(ex -> {
                        ProductException pe = (ProductException) ex;
                        assertThat(pe.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);
                    });
        }
    }
}
