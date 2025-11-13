package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CreateOrderFromCartUseCaseTest {

    private CreateOrderFromCartUseCase useCase;
    private UserMemoryRepository userRepository;
    private CartMemoryRepository cartRepository;
    private ProductMemoryRepository productRepository;
    private PointMemoryRepository pointRepository;

    @BeforeEach
    void setup() {
        userRepository = mock(UserMemoryRepository.class);
        cartRepository = mock(CartMemoryRepository.class);
        productRepository = mock(ProductMemoryRepository.class);
        pointRepository = mock(PointMemoryRepository.class);

        useCase = new CreateOrderFromCartUseCase(
                null, null, userRepository, cartRepository,
                productRepository, null, null, pointRepository, null
        );
    }

    @Test
    void 포인트부족_테스트() {
        Long userId = 1L;
        Long cartId = 1L;
        Long productId = 1L;

        // 1. 사용자 Mock
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // 2. 장바구니 Mock
        Cart cart = mock(Cart.class);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(cart.isSameUser(userId)).thenReturn(true);
        when(cart.getProductId()).thenReturn(productId);
        when(cart.getQuantity()).thenReturn(1);

        // 3. 상품 Mock
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(100));
        when(product.getName()).thenReturn("상품1");
        when(product.canOrder(anyInt())).thenReturn(true);  // 중요: 상품 검증 통과
        when(productRepository.decreaseStockWithLock(productId, 1)).thenReturn(product);

        // 4. 포인트 Mock (사용 가능 포인트 부족)
        Point point = mock(Point.class);
        when(point.getRemainingAmount()).thenReturn(BigDecimal.valueOf(30)); // 포인트 적음
        when(pointRepository.findAvailablePointsByUserId(userId)).thenReturn(List.of(point));

        // 5. 명령 객체 (포인트 사용 금액 50 > 사용 가능 포인트 30)
        CreateOrderFromCartCommand command =
                new CreateOrderFromCartCommand(userId, List.of(cartId), BigDecimal.valueOf(50), null);

        // 6. 예외 발생 검증
        assertThrows(PointException.class, () -> useCase.execute(command));

        // 7. verify
        verify(userRepository).findById(userId);
        verify(cartRepository).findById(cartId);
        verify(productRepository).decreaseStockWithLock(productId, 1);
        verify(pointRepository).findAvailablePointsByUserId(userId);
    }
}
