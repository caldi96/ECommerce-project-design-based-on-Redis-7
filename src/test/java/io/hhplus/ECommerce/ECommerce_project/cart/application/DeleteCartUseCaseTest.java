package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeleteCartUseCaseTest {

    private CartRepository cartRepository;
    private DeleteCartUseCase deleteCartUseCase;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        deleteCartUseCase = new DeleteCartUseCase(cartRepository);
    }

    @Test
    void execute_success() {
        Long cartId = 1L;
        Long userId = 100L;

        // Cart 엔티티는 createCart + setId 사용
        Cart cart = Cart.createCart(userId, 10L, 2);
        cart.setId(cartId);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        deleteCartUseCase.execute(cartId, userId);

        verify(cartRepository).deleteById(cartId);
    }

    @Test
    void execute_cartNotFound_throwsException() {
        Long cartId = 1L;
        Long userId = 100L;

        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteCartUseCase.execute(cartId, userId))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_NOT_FOUND.getMessage());
    }

    @Test
    void execute_wrongUser_throwsException() {
        Long cartId = 1L;
        Long userId = 100L;
        Long otherUserId = 200L;

        Cart cart = Cart.createCart(otherUserId, 10L, 2);
        cart.setId(cartId);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> deleteCartUseCase.execute(cartId, userId))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_ACCESS_DENIED.getMessage());
    }
}
