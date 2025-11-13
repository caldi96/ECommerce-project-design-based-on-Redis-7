package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.cart.presentation.response.GetCartResponse;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GetCartUseCaseTest {

    private CartMemoryRepository cartRepository;
    private ProductMemoryRepository productRepository;
    private GetCartUseCase getCartUseCase;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartMemoryRepository.class);
        productRepository = mock(ProductMemoryRepository.class);
        getCartUseCase = new GetCartUseCase(cartRepository, productRepository);
    }

    @Test
    void execute_success() {
        Long userId = 1L;

        // Cart 엔티티 생성
        Cart cart1 = Cart.createCart(userId, 10L, 2);
        cart1.setId(100L);
        Cart cart2 = Cart.createCart(userId, 20L, 1);
        cart2.setId(101L);

        // Product Mock 생성
        Product product1 = mock(Product.class);
        when(product1.getId()).thenReturn(10L);
        when(product1.getPrice()).thenReturn(BigDecimal.valueOf(1000));

        Product product2 = mock(Product.class);
        when(product2.getId()).thenReturn(20L);
        when(product2.getPrice()).thenReturn(BigDecimal.valueOf(500));

        // Mock Repository 동작 정의
        when(cartRepository.findByUserId(userId)).thenReturn(List.of(cart1, cart2));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product2));

        // 실행
        List<GetCartResponse> responses = getCartUseCase.execute(userId);

        // 검증
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).productId()).isEqualTo(10L);
        assertThat(responses.get(1).productId()).isEqualTo(20L);
    }

    @Test
    void execute_productNotFound_throwsException() {
        Long userId = 1L;

        Cart cart = Cart.createCart(userId, 10L, 2);
        cart.setId(100L);

        when(cartRepository.findByUserId(userId)).thenReturn(List.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCartUseCase.execute(userId))
                .isInstanceOf(ProductException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void execute_emptyCart_returnsEmptyList() {
        Long userId = 1L;

        when(cartRepository.findByUserId(userId)).thenReturn(List.of());

        List<GetCartResponse> responses = getCartUseCase.execute(userId);

        assertThat(responses).isEmpty();
    }
}
