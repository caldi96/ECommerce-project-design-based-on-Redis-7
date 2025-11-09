package io.hhplus.ECommerce.ECommerce_project.cart.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CartEntityTest {

    private Long userId;
    private Long productId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 100L;
        cart = Cart.createCart(userId, productId, 2);
        cart.setId(1L);  // 인메모리 DB용
    }

    @Test
    void createCart_success() {
        assertThat(cart.getUserId()).isEqualTo(userId);
        assertThat(cart.getProductId()).isEqualTo(productId);
        assertThat(cart.getQuantity()).isEqualTo(2);
        assertThat(cart.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void createCart_invalidQuantity() {
        assertThatThrownBy(() -> Cart.createCart(userId, productId, 0))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_QUANTITY_INVALID.getMessage());
    }

    @Test
    void increaseQuantity_success() {
        cart.increaseQuantity();
        assertThat(cart.getQuantity()).isEqualTo(3);

        cart.increaseQuantity(2);
        assertThat(cart.getQuantity()).isEqualTo(5);
    }

    @Test
    void increaseQuantity_invalidAmount() {
        assertThatThrownBy(() -> cart.increaseQuantity(0))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_INCREASE_AMOUNT_INVALID.getMessage());
    }

    @Test
    void decreaseQuantity_success() {
        cart.decreaseQuantity();
        assertThat(cart.getQuantity()).isEqualTo(1);

        cart.changeQuantity(5);
        cart.decreaseQuantity(2);
        assertThat(cart.getQuantity()).isEqualTo(3);
    }

    @Test
    void decreaseQuantity_invalidAmount() {
        assertThatThrownBy(() -> cart.decreaseQuantity(0))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_DECREASE_AMOUNT_INVALID.getMessage());

        assertThatThrownBy(() -> cart.decreaseQuantity(2))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_QUANTITY_CANNOT_BE_LESS_THAN_ONE.getMessage());
    }

    @Test
    void changeQuantity_success() {
        cart.changeQuantity(10);
        assertThat(cart.getQuantity()).isEqualTo(10);
    }

    @Test
    void changeQuantity_invalid() {
        assertThatThrownBy(() -> cart.changeQuantity(0))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_QUANTITY_INVALID.getMessage());
    }

    @Test
    void isSameProductAndUser() {
        assertThat(cart.isSameProduct(productId)).isTrue();
        assertThat(cart.isSameUser(userId)).isTrue();

        assertThat(cart.isSameProduct(999L)).isFalse();
        assertThat(cart.isSameUser(999L)).isFalse();
    }
}
