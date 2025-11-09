package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderFromCartResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class CreateOrderFromProductUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private CreateOrderFromProductUseCase createOrderUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOrderCreation_Success() {
        Long userId = 1L;
        Long productId = 1L;
        int quantity = 2;

        // 유저 생성
        User user = new User(userId, "testUser", "password", BigDecimal.valueOf(1000),
                LocalDateTime.now(), LocalDateTime.now());

        // 상품 생성
        Product product = Product.createProduct("테스트 상품", 1L, "설명", BigDecimal.valueOf(100), 10, 1, 5);
        product.setId(productId);

        // Mock 설정
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenAnswer(i -> {
            Orders order = i.getArgument(0);
            order.setId(1L); // 테스트용 ID 설정
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(pointRepository.findAvailablePointsByUserId(userId)).thenReturn(List.of());

        // 커맨드 생성
        CreateOrderFromProductCommand command = new CreateOrderFromProductCommand(
                userId, productId, quantity, null, null
        );

        // 실행
        CreateOrderFromCartResponse response = createOrderUseCase.execute(command);

        // 검증
        assertNotNull(response);
        assertEquals(quantity, response.orderItems().get(0).quantity());
        assertEquals(product.getPrice(), response.orderItems().get(0).unitPrice());
        verify(userRepository, never()).save(any()); // 포인트 미사용
    }

}
