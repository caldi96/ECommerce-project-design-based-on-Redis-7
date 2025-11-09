package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.IncreaseStockCommand;
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

public class IncreaseStockUseCaseTest {

    private ProductRepository productRepository;
    private IncreaseStockUseCase increaseStockUseCase;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        increaseStockUseCase = new IncreaseStockUseCase(productRepository);
    }

    @Test
    void execute_whenProductNotFound_thenThrowException() {
        // given
        IncreaseStockCommand command = new IncreaseStockCommand(1L, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        ProductException exception = assertThrows(ProductException.class,
                () -> increaseStockUseCase.execute(command));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void execute_whenProductExists_thenIncreaseStockAndSave() {
        // given
        Product product = Product.createProduct(
                "Test Product",
                10L,
                "Description",
                BigDecimal.valueOf(1000),
                5,      // 기존 재고
                1,
                10
        );
        product.setId(1L);

        IncreaseStockCommand command = new IncreaseStockCommand(1L, 3);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = increaseStockUseCase.execute(command);

        // then
        assertThat(result.getStock()).isEqualTo(8); // 5 + 3 = 8

        // save가 호출되었는지 검증
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();
        assertThat(savedProduct.getStock()).isEqualTo(8);
    }
}
