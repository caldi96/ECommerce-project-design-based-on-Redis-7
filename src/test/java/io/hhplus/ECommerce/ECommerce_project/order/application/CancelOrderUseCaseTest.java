package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CancelOrderCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private PointRepository pointRepository;
    @Mock
    private PointUsageHistoryRepository pointUsageHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CancelOrderUseCase cancelOrderUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_cancelPendingOrder_success() {
        // given
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), null, null, null, null);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L);

        Product product = mock(Product.class);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(
                OrderItem.createOrderItem(1L, 10L, "Product A", 1, BigDecimal.valueOf(1000))
        ));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // when
        cancelOrderUseCase.execute(command);

        // then
        assertThat(order.isCanceled()).isTrue();
        verify(product).increaseStock(1);
        verify(product).decreaseSoldCount(1);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void execute_cancelPaidOrder_success() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), null, null, null, null);
        order.paid();
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L);

        Product product = mock(Product.class);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(
                OrderItem.createOrderItem(1L, 10L, "Product A", 1, BigDecimal.valueOf(1000))
        ));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        cancelOrderUseCase.execute(command);

        assertThat(order.isCanceled()).isTrue();
        verify(product).increaseStock(1);
        verify(product).decreaseSoldCount(1);
        verify(productRepository).save(product);
        verify(orderRepository).save(order);
    }

    @Test
    void execute_orderNotFound_throwsException() {
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(OrderException.class, () -> cancelOrderUseCase.execute(command));
    }

    @Test
    void execute_wrongUser_throwsException() {
        Orders order = Orders.createOrder(2L, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), null, null, null, null);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(OrderException.class, () -> cancelOrderUseCase.execute(command));
    }

    @Test
    void execute_withCouponAndPoints_success() {
        // given
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), 5L, BigDecimal.valueOf(100), BigDecimal.valueOf(50), null);
        CancelOrderCommand command = new CancelOrderCommand(1L, 1L);

        Product product = mock(Product.class);
        UserCoupon userCoupon = mock(UserCoupon.class);
        Coupon coupon = mock(Coupon.class);
        PointUsageHistory pointHistory = mock(PointUsageHistory.class);
        Point point = mock(Point.class);
        User user = mock(User.class);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(
                OrderItem.createOrderItem(1L, 10L, "Product A", 1, BigDecimal.valueOf(1000))
        ));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userCouponRepository.findByUserIdAndCouponId(1L, 5L)).thenReturn(Optional.of(userCoupon));
        when(couponRepository.findById(5L)).thenReturn(Optional.of(coupon));
        when(pointUsageHistoryRepository.findByOrderIdAndCanceledAtIsNull(1L)).thenReturn(List.of(pointHistory));
        when(pointRepository.findById(anyLong())).thenReturn(Optional.of(point));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(pointHistory.getPointId()).thenReturn(100L);
        when(pointHistory.getUsedAmount()).thenReturn(BigDecimal.valueOf(50));

        // when
        cancelOrderUseCase.execute(command);

        // then
        verify(userCoupon).cancelUse(anyInt());
        verify(userCouponRepository).save(userCoupon);
        verify(coupon).decreaseUsageCount();
        verify(couponRepository).save(coupon);
        verify(point).restoreUsedAmount(BigDecimal.valueOf(50));
        verify(pointRepository).save(point);
        verify(pointHistory).cancel();
        verify(pointUsageHistoryRepository).save(pointHistory);
        verify(user).refundPoint(BigDecimal.valueOf(50));
        verify(userRepository).save(user);
        assertThat(order.isCanceled()).isTrue();
    }
}
