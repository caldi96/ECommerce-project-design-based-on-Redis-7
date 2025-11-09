package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class GetProductUseCaseTest {

    private ProductRepository productRepository;
    private GetProductUseCase getProductUseCase;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        getProductUseCase = new GetProductUseCase(productRepository);
    }

    @Test
    void execute_whenProductNotFound_thenThrowException() {
        // given
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        ProductException exception = assertThrows(ProductException.class,
                () -> getProductUseCase.execute(productId));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void execute_whenProductExists_thenIncreaseViewCountAndSave() {
        // given
        Product product = Product.createProduct(
                "Test Product",
                10L,
                "Description",
                BigDecimal.valueOf(1000),
                5,
                1,
                10
        );
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = getProductUseCase.execute(1L);

        // then
        assertThat(result.getViewCount()).isEqualTo(1);  // 조회수 증가 확인

        // save가 호출되었는지 확인
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();
        assertThat(savedProduct.getViewCount()).isEqualTo(1);
    }
}
