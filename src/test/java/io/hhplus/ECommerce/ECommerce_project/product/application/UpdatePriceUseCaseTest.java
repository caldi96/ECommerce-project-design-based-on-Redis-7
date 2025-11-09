package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.UpdatePriceCommand;
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

public class UpdatePriceUseCaseTest {

    private ProductRepository productRepository;
    private UpdatePriceUseCase updatePriceUseCase;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        updatePriceUseCase = new UpdatePriceUseCase(productRepository);
    }

    @Test
    void execute_whenProductNotFound_thenThrowException() {
        // given
        UpdatePriceCommand command = new UpdatePriceCommand(1L, BigDecimal.valueOf(2000));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        ProductException exception = assertThrows(ProductException.class,
                () -> updatePriceUseCase.execute(command));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void execute_whenProductExists_thenUpdatePriceAndSave() {
        // given
        Product product = Product.createProduct(
                "Test Product",
                10L,
                "Description",
                BigDecimal.valueOf(1000), // 기존 가격
                5,
                1,
                10
        );
        product.setId(1L);

        UpdatePriceCommand command = new UpdatePriceCommand(1L, BigDecimal.valueOf(2000));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = updatePriceUseCase.execute(command);

        // then
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));

        // save가 호출되었는지 검증
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }
}
