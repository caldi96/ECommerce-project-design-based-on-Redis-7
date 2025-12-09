package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponUsageService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromCartData;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromProductData;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointUsageService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCompletionService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final UserFinderService userFinderService;
    private final ProductFinderService productFinderService;
    private final UserCouponUsageService userCouponUsageService;
    private final PointUsageService pointUsageService;

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CreateOrderResponse completeOrderFromProduct(
            CreateOrderFromProductCommand command,
            ValidatedOrderFromProductData validatedOrderFromProductData
    ) {

        // 1. 사용자 확인 (낙관적 락 - 본인만 접근하는 리소스)
        User user = userFinderService.getUser(command.userId());

        // 2. 상품 락 없이 조회 (OrderItem 생성용)
        Product product = productFinderService.getProduct(command.productId());

        // 3. 쿠폰 사용 처리 (서비스에 위임)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (command.couponId() != null) {
            coupon = userCouponUsageService.useCoupon(command.userId(), command.couponId());
            discountAmount = validatedOrderFromProductData.discountAmount();
        }

        // 4. Order 생성 (최종 금액은 Order에서 create할때 계산)
        Orders order = Orders.createOrder(
                user,
                coupon,
                validatedOrderFromProductData.totalAmount(),
                validatedOrderFromProductData.shippingFee(),
                discountAmount,
                command.pointAmount()
        );

        // 5. 저장
        Orders savedOrder = orderRepository.save(order);

        // 6. 포인트 사용 처리 (서비스에 위임)
        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {
            pointUsageService.usePoints(
                    command.userId(),
                    command.pointAmount(),
                    savedOrder
            );
        }

        // 7. OrderItem 생성
        OrderItem orderItem = OrderItem.createOrderItem(
                savedOrder,
                product,
                product.getName(),
                command.quantity(),
                product.getPrice()
        );
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        List<OrderItem> orderItems = List.of(savedOrderItem);

        return CreateOrderResponse.from(savedOrder, orderItems);
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CreateOrderResponse completeOrderFromCart(
            CreateOrderFromCartCommand command,
            ValidatedOrderFromCartData validatedOrderFromCartData
    ) {

        // 1. 사용자 확인 (낙관적 락 - 본인만 접근하는 리소스)
        User user = userFinderService.getUser(command.userId());

        // 2. 쿠폰 처리 (서비스에 위임)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (command.couponId() != null) {
            coupon = userCouponUsageService.useCoupon(command.userId(), command.couponId());
            discountAmount = validatedOrderFromCartData.discountAmount();
        }

        // 3. Order 생성 (최종 금액은 Order에서 create할때 계산)
        Orders order = Orders.createOrder(
                user,
                coupon,
                validatedOrderFromCartData.totalAmount(),            // 상품 총액
                validatedOrderFromCartData.shippingFee(),            // 배송비
                discountAmount,         // 쿠폰 할인 금액
                command.pointAmount()   // 포인트 사용 금액
        );

        // 4. 저장
        Orders savedOrder = orderRepository.save(order);

        // 5. 포인트 사용 처리 (서비스에 위임)
        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {
            pointUsageService.usePoints(
                    command.userId(),
                    command.pointAmount(),
                    savedOrder
            );
        }

        // 6. OrderItem 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : validatedOrderFromCartData.cartList()) {
            Product product = validatedOrderFromCartData.productMap().get(cart.getProduct().getId());

            OrderItem orderItem = OrderItem.createOrderItem(
                    savedOrder,
                    product,
                    product.getName(),
                    cart.getQuantity(),
                    product.getPrice()
            );
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        // 7. 장바구니 삭제 (물리 삭제)
        for (Cart cart : validatedOrderFromCartData.cartList()) {
            cartRepository.deleteById((cart.getId()));
        }

        return CreateOrderResponse.from(savedOrder, orderItems);
    }
}