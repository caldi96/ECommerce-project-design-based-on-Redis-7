package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.command.UpdateQuantityCommand;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.cart.presentation.response.UpdateQuantityResponse;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateQuantityUseCaseTest {

    private CartMemoryRepository cartRepository;
    private ProductMemoryRepository productRepository;
    private UpdateQuantityUseCase updateQuantityUseCase;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartMemoryRepository.class);
        productRepository = mock(ProductMemoryRepository.class);
        updateQuantityUseCase = new UpdateQuantityUseCase(cartRepository, productRepository);
    }

    @Test
    void execute_success() {
        Long userId = 1L;
        Long cartId = 100L;
        Long productId = 10L;
        int newQuantity = 5;

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartId, userId, newQuantity);

        Cart cart = Cart.createCart(userId, productId, 2);
        cart.setId(cartId);

        Product product = mock(Product.class);
        when(product.canOrder(newQuantity)).thenReturn(true);
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(1000));
        when(product.getId()).thenReturn(productId);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(cart)).thenReturn(cart);

        UpdateQuantityResponse response = updateQuantityUseCase.execute(command);

        assertThat(cart.getQuantity()).isEqualTo(newQuantity);
        assertThat(response.quantity()).isEqualTo(newQuantity);
        verify(cartRepository).save(cart);
    }

    @Test
    void execute_cartNotFound_throwsException() {
        UpdateQuantityCommand command = new UpdateQuantityCommand(100L, 1L, 5);

        when(cartRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateQuantityUseCase.execute(command))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_NOT_FOUND.getMessage());
    }

    @Test
    void execute_wrongUser_throwsException() {
        Long cartId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartId, userId, 5);

        Cart cart = Cart.createCart(otherUserId, 10L, 2);
        cart.setId(cartId);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> updateQuantityUseCase.execute(command))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_ACCESS_DENIED.getMessage());
    }

    @Test
    void execute_productNotFound_throwsException() {
        Long cartId = 100L;
        Long userId = 1L;

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartId, userId, 5);

        Cart cart = Cart.createCart(userId, 10L, 2);
        cart.setId(cartId);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateQuantityUseCase.execute(command))
                .isInstanceOf(ProductException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void execute_quantityExceedsStock_throwsException() {
        Long cartId = 100L;
        Long userId = 1L;

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartId, userId, 100);

        Cart cart = Cart.createCart(userId, 10L, 2);
        cart.setId(cartId);

        Product product = mock(Product.class);
        when(product.canOrder(100)).thenReturn(false);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> updateQuantityUseCase.execute(command))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_PRODUCT_CANNOT_BE_ADDED_TO_CART.getMessage());
    }
}
