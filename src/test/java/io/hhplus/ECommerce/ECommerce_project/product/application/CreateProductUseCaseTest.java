package io.hhplus.ECommerce.ECommerce_project.product.application;

import io.hhplus.ECommerce.ECommerce_project.product.application.command.CreateProductCommand;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateProductUseCaseTest {
    @Mock
    ProductRepository productRepository;

    @InjectMocks
    CreateProductUseCase createProductUseCase;

    @Test
    void 상품을_정상적으로_생성한다() {
        // given
        CreateProductCommand command = new CreateProductCommand(
                "상품명",
                1L,
                "설명",
                BigDecimal.valueOf(1000),
                10,
                1,
                10
        );

        Product savedProduct = Product.createProduct(
                command.name(),
                command.categoryId(),
                command.description(),
                command.price(),
                command.stock(),
                command.minOrderQuantity(),
                command.maxOrderQuantity()
        );
        savedProduct.setId(1L); // 인메모리 저장 시 ID 부여

        given(productRepository.save(any(Product.class)))
                .willReturn(savedProduct);

        // when
        Product result = createProductUseCase.execute(command);

        // then
        verify(productRepository).save(any(Product.class));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("상품명");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(1000));
    }
}
