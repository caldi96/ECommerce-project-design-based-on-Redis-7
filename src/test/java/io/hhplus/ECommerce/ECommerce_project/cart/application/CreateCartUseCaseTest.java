package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.command.CreateCartCommand;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CreateCartUseCaseTest {

    private CartMemoryRepository cartRepository;
    private ProductMemoryRepository productRepository;
    private UserMemoryRepository userRepository;
    private CreateCartUseCase createCartUseCase;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartMemoryRepository.class);
        productRepository = mock(ProductMemoryRepository.class);
        userRepository = mock(UserMemoryRepository.class);

        createCartUseCase = new CreateCartUseCase(cartRepository, productRepository, userRepository);
    }

    @Test
    void execute_createNewCart_success() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 2;

        CreateCartCommand command = new CreateCartCommand(userId, productId, quantity);

        User user = mock(User.class);
        Product product = mock(Product.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(product.canOrder(quantity)).thenReturn(true);
        when(cartRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        Cart savedCart = Cart.createCart(userId, productId, quantity);
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);

        // when
        Cart result = createCartUseCase.execute(command);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getQuantity()).isEqualTo(quantity);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void execute_existingCart_increaseQuantity() {
        // given
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 3;

        CreateCartCommand command = new CreateCartCommand(userId, productId, quantity);

        User user = mock(User.class);
        Product product = mock(Product.class);
        Cart existingCart = Cart.createCart(userId, productId, 2);
        existingCart.setId(100L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(product.canOrder(anyInt())).thenReturn(true);
        when(cartRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(existingCart)).thenReturn(existingCart);

        // when
        Cart result = createCartUseCase.execute(command);

        // then
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartRepository).save(existingCart);
    }

    @Test
    void execute_userNotFound_throwsException() {
        CreateCartCommand command = new CreateCartCommand(1L, 10L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createCartUseCase.execute(command))
                .isInstanceOf(UserException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void execute_productNotFound_throwsException() {
        CreateCartCommand command = new CreateCartCommand(1L, 10L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createCartUseCase.execute(command))
                .isInstanceOf(ProductException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @Test
    void execute_productCannotOrder_throwsException() {
        CreateCartCommand command = new CreateCartCommand(1L, 10L, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        Product product = mock(Product.class);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(product.canOrder(2)).thenReturn(false);

        assertThatThrownBy(() -> createCartUseCase.execute(command))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_PRODUCT_CANNOT_BE_ADDED_TO_CART.getMessage());
    }

    @Test
    void execute_existingCart_cannotOrderAdditionalQuantity_throwsException() {
        Long userId = 1L;
        Long productId = 10L;
        int quantity = 5;

        CreateCartCommand command = new CreateCartCommand(userId, productId, quantity);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        Product product = mock(Product.class);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Cart existingCart = Cart.createCart(userId, productId, 3);
        when(cartRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existingCart));
        when(product.canOrder(3 + quantity)).thenReturn(false);

        assertThatThrownBy(() -> createCartUseCase.execute(command))
                .isInstanceOf(CartException.class)
                .hasMessage(ErrorCode.CART_PRODUCT_CANNOT_BE_ADDED_TO_CART.getMessage());
    }
}
