package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductMemoryRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserMemoryRepository;
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
    private OrderMemoryRepository orderRepository;

    @Mock
    private OrderItemMemoryRepository orderItemRepository;

    @Mock
    private UserMemoryRepository userRepository;

    @Mock
    private ProductMemoryRepository productRepository;

    @Mock
    private PointMemoryRepository pointRepository;

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
        User user = new User("testUser", "password", BigDecimal.valueOf(1000),
                LocalDateTime.now(), LocalDateTime.now());
        user.setId(userId);

        // 상품 생성
        Product product = Product.createProduct("테스트 상품", 1L, "설명", BigDecimal.valueOf(100), 10, 1, 5);
        product.setId(productId);

        // Mock 설정
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.decreaseStockWithLock(productId, quantity)).thenReturn(product);
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
        CreateOrderResponse response = createOrderUseCase.execute(command);

        // 검증
        assertNotNull(response);
        assertEquals(quantity, response.orderItems().get(0).quantity());
        assertEquals(product.getPrice(), response.orderItems().get(0).unitPrice());
        verify(userRepository, never()).save(any()); // 포인트 미사용
    }

}
