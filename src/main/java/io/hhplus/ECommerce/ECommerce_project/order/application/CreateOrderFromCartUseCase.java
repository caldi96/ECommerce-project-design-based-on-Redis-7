package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromCartData;
import io.hhplus.ECommerce.ECommerce_project.order.domain.constants.ShippingPolicy;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.infrastructure.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.StockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateOrderFromCartUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;
    private final StockService stockService;

    public CreateOrderResponse execute(CreateOrderFromCartCommand command) {

        // 1. 검증 및 사전 계산 (트랜잭션 밖)
        ValidatedOrderFromCartData validatedOrderFromCartData = validateAndCalculate((command));

        // 2. 재고 차감 (트랜잭션 1)
        stockService.reserveStocks(validatedOrderFromCartData.sortedEntries());

        try {
            // 3. 주문 완료 (트랜잭션 2)
            return completeOrder(command, validatedOrderFromCartData);
        } catch (Exception e) {
            // 4. 실패 시 재고 복구
            stockService.compensateStocks(validatedOrderFromCartData.sortedEntries());
            throw e;
        }
    }

    private ValidatedOrderFromCartData validateAndCalculate(CreateOrderFromCartCommand command) {
        // 1. 사용자 확인
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 장바구니 아이템 조회 (cartItemIds)
        List<Cart> cartList = command.cartItemIds().stream()
                .map(cartId -> {
                    Cart cart = cartRepository.findById(cartId)
                            .orElseThrow(() -> new CartException(ErrorCode.CART_NOT_FOUND));

                    // 유저의 카트인지 확인
                    if (!cart.isSameUser(command.userId())) {
                        throw new CartException(ErrorCode.CART_ACCESS_DENIED);
                    }

                    return cart;
                })
                .toList();

        // 3. 각 장바구니 아이템에 대해 상품 검증 및 재고 처리
        // 3-1. 상품별 주문 수량 집계 (같은 상품이 여러 장바구니 항목에 있을 수 있음)
        Map<Long, Integer> productOrderQuantityMap = new HashMap<>();
        for (Cart cart : cartList) {
            productOrderQuantityMap.merge(
                    cart.getProduct().getId(),
                    cart.getQuantity(),
                    Integer::sum
            );
        }

        // 3-2. 장바구니 아이템 오름차순 정렬 (데드락 방지: productId 오름차순 정렬, 원자적 처리)
        // 발생 가능한 데드락 예시 : A가 1번 상품 -> 2번 상품, B가 2번 상품 -> 1번 상품  이런 경우 서로 상품을 점유한 상태에서 안 놔줘서 안 넘어감
        List<Map.Entry<Long, Integer>> sortedEntries = productOrderQuantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        Map<Long, Product> productMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // 상품 조회 및 검증 (락 없이)
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 주문 가능 여부 검증 (비활성/재고/최소/최대 주문량 체크)
            product.validateOrder(totalQuantity);

            // 주문 금액 계산을 위해 productMap에 저장
            productMap.put(productId, product);
        }

        // 4. 주문 금액 계산
        BigDecimal totalAmount = calculateTotalAmount(cartList, productMap);

        // 5. 배송비 계산 (상수 클래스 사용)
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 6. 쿠폰 처리
        BigDecimal discountAmount = BigDecimal.ZERO;

        Coupon coupon = null;

        if (command.couponId() != null) {
            // 6-1. 사용자 쿠폰 조회
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 조회 및 검증
            coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 6-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 6-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 6-5. 할인 금액 계산 (최소 주문 금액 검증 포함)
            discountAmount = coupon.calculateDiscountAmount(totalAmount);
        }

        // 7. 포인트 사용 (포인트 사용시에만)
        BigDecimal pointAmount = BigDecimal.ZERO;
        List<Point> pointsToUpdate = new ArrayList<>();  // 사용될 포인트들
        List<BigDecimal> pointUsageAmounts = new ArrayList<>();  // 각 포인트에서 사용할 금액

        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {

            // 사용 가능한 포인트 조회
            List<Point> availablePoints = pointRepository.findAvailablePointsByUserId(command.userId());

            // 사용 가능한 포인트 합계 계산 (남은 금액 기준)
            BigDecimal totalAvailablePoint = availablePoints.stream()
                    .map(Point::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 포인트 잔액 검증
            if (totalAvailablePoint.compareTo(command.pointAmount()) < 0) {
                throw new PointException(ErrorCode.POINT_INSUFFICIENT_POINT);
            }
        }

        return new ValidatedOrderFromCartData(
                cartList,
                sortedEntries,
                productMap,
                totalAmount,
                shippingFee,
                discountAmount
        );
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CreateOrderResponse completeOrder(
            CreateOrderFromCartCommand command,
            ValidatedOrderFromCartData validatedOrderFromCartData
    ) {

        // 1. 사용자 확인 (낙관적 락 - 본인만 접근하는 리소스)
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 쿠폰 처리
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (command.couponId() != null) {
            // 2-1. 사용자 쿠폰 조회 (낙관적 락 - 본인만 접근하는 리소스)
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 2-2. 쿠폰 조회 및 검증
            coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 2-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 2-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 2-5. 할인 금액 계산
            discountAmount = validatedOrderFromCartData.discountAmount();

            // 2-6. 쿠폰 사용 처리 (usedCount 증가)
            userCoupon.use(coupon.getPerUserLimit());
            userCouponRepository.save(userCoupon);
        }

        // 3. 포인트 사용 (포인트 사용시에만)
        BigDecimal pointAmount = BigDecimal.ZERO;
        List<Point> pointsToUpdate = new ArrayList<>();  // 사용될 포인트들
        List<BigDecimal> pointUsageAmounts = new ArrayList<>();  // 각 포인트에서 사용할 금액

        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {

            // 사용 가능한 포인트 조회
            List<Point> availablePoints = pointRepository.findAvailablePointsByUserId(command.userId());

            // 포인트 사용 처리 (선입선출, 낙관적 락 - 본인만 접근하는 리소스)
            BigDecimal remainingPointToUse = command.pointAmount();
            for (Point point : availablePoints) {
                if (remainingPointToUse.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // 해당 포인트에서 사용할 수 있는 금액 계산
                BigDecimal availableAmount = point.getRemainingAmount();
                BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

                // 나중에 사용 이력 생성을 위해 임시 저장
                pointUsageAmounts.add(pointToUse);
                pointsToUpdate.add(point);

                remainingPointToUse = remainingPointToUse.subtract(pointToUse);
            }

            pointAmount = command.pointAmount();
        }

        // 4. Order 생성 (최종 금액은 Order에서 create할때 계산)
        Orders order = Orders.createOrder(
                user,
                coupon,
                validatedOrderFromCartData.totalAmount(),            // 상품 총액
                validatedOrderFromCartData.shippingFee(),            // 배송비
                discountAmount,         // 쿠폰 할인 금액
                pointAmount             // 포인트 사용 금액
        );

        // 5. 저장
        Orders savedOrder = orderRepository.save(order);

        // 6. 포인트 사용 이력 저장 (Order ID가 필요하므로 주문 생성 후 처리)
        if (!pointUsageAmounts.isEmpty()) {
            for (int i = 0; i < pointUsageAmounts.size(); i++) {
                BigDecimal usageAmount = pointUsageAmounts.get(i);

                // 6-1. 기존 CHARGE/REFUND 포인트는 부분 사용 처리
                Point originalPoint = pointsToUpdate.get(i);
                originalPoint.usePartially(usageAmount);
                pointRepository.save(originalPoint);

                // 6-2. PointUsageHistory 생성 (주문과 포인트 연결 추적용)
                PointUsageHistory history = PointUsageHistory.create(
                        originalPoint,
                        savedOrder,
                        usageAmount
                );
                // 6-3. 포인트 사용 내역 저장
                pointUsageHistoryRepository.save(history);
            }

            if (pointAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 6-4. User의 포인트 잔액 차감
                user.usePoint(pointAmount);
                userRepository.save(user);
            }
        }

        // 7. OrderItem 생성
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

        // 8. 장바구니 삭제 (물리 삭제)
        for (Cart cart : validatedOrderFromCartData.cartList()) {
            cartRepository.deleteById((cart.getId()));
        }

        return CreateOrderResponse.from(savedOrder, orderItems);
    }

    // 주문 금액 계산 헬퍼 메서드
    private BigDecimal calculateTotalAmount(List<Cart> cartList, Map<Long, Product> productMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProduct().getId());

            BigDecimal itemTotalAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemTotalAmount);
        }

        return totalAmount;
    }
}
