package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.application.command.UpdateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class UpdateProductUseCaseTest {

    private ProductMemoryRepository productRepository;
    private UpdateProductUseCase updateProductUseCase;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductMemoryRepository.class);
        updateProductUseCase = new UpdateProductUseCase(productRepository);
    }

    @Test
    void execute_whenProductNotFound_thenThrowException() {
        // given
        UpdateProductCommand command = new UpdateProductCommand(
                1L, 20L, "New Name", "New Description", BigDecimal.valueOf(2000),
                true, 2, 15
        );
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        ProductException exception = assertThrows(ProductException.class,
                () -> updateProductUseCase.execute(command));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void execute_whenProductExists_thenUpdateFieldsAndSave() {
        // given
        Product product = Product.createProduct(
                "Old Name",
                10L,
                "Old Description",
                BigDecimal.valueOf(1000),
                5,
                1,
                10
        );
        product.setId(1L);

        UpdateProductCommand command = new UpdateProductCommand(
                1L, 20L, "New Name", "New Description", BigDecimal.valueOf(2000),
                false, 2, 15
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = updateProductUseCase.execute(command);

        // then
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New Description");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.getCategoryId()).isEqualTo(20L);
        assertThat(result.getMinOrderQuantity()).isEqualTo(2);
        assertThat(result.getMaxOrderQuantity()).isEqualTo(15);
        assertThat(result.isActive()).isFalse();

        // save가 호출되었는지 검증
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();
        assertThat(savedProduct).isEqualTo(result);
    }

    @Test
    void execute_whenActiveStatusTrue_thenActivateProduct() {
        // given
        Product product = Product.createProduct(
                "Old Name",
                10L,
                "Old Description",
                BigDecimal.valueOf(1000),
                5,
                1,
                10
        );
        product.setId(1L);
        product.deactivate(); // 비활성화 상태

        UpdateProductCommand command = new UpdateProductCommand(
                1L, null, null, null, null,
                true, null, null
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = updateProductUseCase.execute(command);

        // then
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void execute_whenActiveStatusFalse_thenDeactivateProduct() {
        // given
        Product product = Product.createProduct(
                "Old Name",
                10L,
                "Old Description",
                BigDecimal.valueOf(1000),
                5,
                1,
                10
        );
        product.setId(1L);
        // 기본적으로 활성 상태

        UpdateProductCommand command = new UpdateProductCommand(
                1L, null, null, null, null,
                false, null, null
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Product result = updateProductUseCase.execute(command);

        // then
        assertThat(result.isActive()).isFalse();
    }
}
