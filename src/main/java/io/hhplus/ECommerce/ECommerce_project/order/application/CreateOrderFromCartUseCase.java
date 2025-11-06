package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.constants.ShippingPolicy;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderFromCartResponse;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.repository.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final PaymentRepository paymentRepository;

    @Transactional
    public CreateOrderFromCartResponse execute(CreateOrderFromCartCommand command) {
        // 보상 트랜잭션을 위한 컨테이너 클래스
        RollbackContext rollbackContext = new RollbackContext();

        try {
            return executeOrder(command, rollbackContext);
        } catch (Exception e) {
            // 보상 트랜잭션 실행
            compensateTransaction(rollbackContext);
            throw e;
        }
    }

    // 추후 jpa 사용시 보상 트랜잭션 제거
    // 롤백 정보를 담는 컨테이너 클래스
    private static class RollbackContext {
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        UserCoupon userCoupon = null;
        Coupon coupon = null;
        List<Point> points = new ArrayList<>();
        Map<Point, BigDecimal> pointUsageMap = new HashMap<>(); // 포인트별 사용 금액 저장
        User user = null; // 포인트 사용한 사용자
        BigDecimal usedPointAmount = BigDecimal.ZERO; // 사용한 총 포인트 금액
    }

    private CreateOrderFromCartResponse executeOrder(
            CreateOrderFromCartCommand command,
            RollbackContext rollbackContext) {

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
                cart.getProductId(),
                cart.getQuantity(),
                Integer::sum
            );
        }

        // 3-2. 상품 조회 및 검증 (모든 상품에 대해 먼저 검증)
        // 동시성 제어를 위해 락을 걸고 조회
        Map<Long, Product> productMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : productOrderQuantityMap.entrySet()) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // 상품 조회 (비관적 락 적용)
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 상품 주문 가능 여부 확인 (활성화, 재고)
            if (!product.canOrder(totalQuantity)) {
                throw new OrderException(ErrorCode.ORDER_PRODUCT_CANNOT_BE_ORDERED);
            }

            productMap.put(productId, product);
        }

        // 3-3. 모든 검증이 완료된 후 재고 차감 및 판매량 증가
        for (Map.Entry<Long, Integer> entry : productOrderQuantityMap.entrySet()) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();
            Product product = productMap.get(productId);

            // 재고 차감 (동시성 처리 필요!)
            product.decreaseStock(totalQuantity);

            // 판매량 증가
            product.increaseSoldCount(totalQuantity);

            // 변경사항 저장
            productRepository.save(product);

            // 롤백을 위한 정보 저장
            rollbackContext.productQuantityMap.put(productId, totalQuantity);
        }

        // 4. 주문 금액 계산
        BigDecimal totalAmount = calculateTotalAmount(cartList, productMap);

        // 5. 배송비 계산 (상수 클래스 사용)
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 6. 쿠폰 처리
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (command.couponId() != null) {
            // 6-1. 사용자 쿠폰 조회
            UserCoupon userCoupon = userCouponRepository
                    .findByUserIdAndCouponId(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 6-3. 쿠폰 유효성 검증 (활성화, 사용 가능 기간 확인)
            coupon.validateAvailability();

            // 6-4. 사용자 쿠폰 사용 가능 여부
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 6-5. 할인 금액 계산 (최소 주문 금액 검증 포함)
            discountAmount = coupon.calculateDiscountAmount(totalAmount);

            // 6-6. 쿠폰 사용 처리
            userCoupon.use(coupon.getPerUserLimit());
            userCouponRepository.save(userCoupon);

            // 6-7. 쿠폰 사용 횟수 증가
            coupon.increaseUsageCount();
            couponRepository.save(coupon);

            // 롤백을 위한 정보 저장
            rollbackContext.userCoupon = userCoupon;
            rollbackContext.coupon = coupon;
        }

        // 7. 포인트 사용 (있으면) - 임시 저장용 리스트
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

            // 포인트 사용 처리 (선입선출) - Order ID가 필요하므로 임시 저장
            BigDecimal remainingPointToUse = command.pointAmount();
            for (Point point : availablePoints) {
                if (remainingPointToUse.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // 해당 포인트에서 사용할 수 있는 금액 계산
                BigDecimal availableAmount = point.getRemainingAmount();
                BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

                // 나중에 사용 이력 생성을 위해 임시 저장
                pointsToUpdate.add(point);
                pointUsageAmounts.add(pointToUse);

                remainingPointToUse = remainingPointToUse.subtract(pointToUse);
            }

            pointAmount = command.pointAmount();
        }

        // 8. Order 생성 (최종 금액은 Order에서 create할때 계산)
        List<Long> usedPointIds = pointsToUpdate.stream()
                .map(Point::getId)
                .toList();

        Orders order = Orders.createOrder(
                command.userId(),
                totalAmount,            // 상품 총액
                shippingFee,            // 배송비
                command.couponId(),     // 쿠폰 ID
                discountAmount,         // 쿠폰 할인 금액
                pointAmount,            // 포인트 사용 금액
                usedPointIds            // 사용한 포인트 ID 리스트
        );

        // 9. 저장
        Orders savedOrder = orderRepository.save(order);

        // 10. 포인트 사용 이력 저장 (Order ID가 필요하므로 주문 생성 후 처리)
        if (!pointUsageAmounts.isEmpty()) {
            for (int i = 0; i < pointUsageAmounts.size(); i++) {
                BigDecimal usageAmount = pointUsageAmounts.get(i);

                // 10-1. USE 타입 포인트 생성 (사용 이력 기록용)
                Point usedPoint = Point.use(
                    command.userId(),
                    usageAmount,
                    "주문 결제"
                );
                pointRepository.save(usedPoint);

                // 10-2. 기존 CHARGE/REFUND 포인트는 부분 사용 처리
                Point originalPoint = pointsToUpdate.get(i);
                originalPoint.usePartially(usageAmount);
                pointRepository.save(originalPoint);

                // 10-3. PointUsageHistory 생성 (주문과 포인트 연결 추적용)
                PointUsageHistory history = PointUsageHistory.create(
                    originalPoint.getId(),
                    savedOrder.getId(),
                    usageAmount
                );
                pointUsageHistoryRepository.save(history);

                // 10-4. 롤백을 위한 정보 저장
                rollbackContext.points.add(originalPoint);
                rollbackContext.pointUsageMap.put(originalPoint, usageAmount);
            }

            // 10-5. User의 포인트 잔액 차감
            user.usePoint(pointAmount);
            userRepository.save(user);

            // 10-6. 롤백을 위한 User 정보 저장
            rollbackContext.user = user;
            rollbackContext.usedPointAmount = pointAmount;
        }

        // 11. OrderItem 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());

            OrderItem orderItem = OrderItem.createOrderItem(
                    savedOrder.getId(),
                    product.getId(),
                    product.getName(),
                    cart.getQuantity(),
                    product.getPrice()
            );
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        // 12. 장바구니 삭제 (물리 삭제)
        for (Cart cart : cartList) {
            cartRepository.deleteById((cart.getId()));
        }

        // 13. 결제 정보 생성
        Payment payment = Payment.createPayment(
                savedOrder.getId(),
                savedOrder.getFinalAmount(),
                PaymentMethod.CARD
        );

        // 14. 결제 처리 (실제로는 외부 결제 API 호출)
        // TODO: 실제 결제 API 연동 시 이 부분 구현
        try {
            // 외부 결제 API 호출 시뮬레이션
            // boolean paymentSuccess = externalPaymentAPI.process(payment);

            // 현재는 항상 성공으로 처리 (테스트용)
            payment.complete();
            Payment savedPayment = paymentRepository.save(payment);

            // 15. 주문 상태를 PAID로 변경
            savedOrder.paid();
            orderRepository.save(savedOrder);

            return CreateOrderFromCartResponse.from(savedOrder, savedPayment, orderItems);

        } catch (Exception e) {
            // 결제 실패 처리
            payment.fail(e.getMessage());
            paymentRepository.save(payment);

            // 주문 상태를 PAYMENT_FAILED로 변경
            savedOrder.paymentFailed();
            orderRepository.save(savedOrder);

            // 예외를 다시 던져서 보상 트랜잭션이 실행되도록 함
            throw new PaymentException(ErrorCode.PAYMENT_ALREADY_FAILED,
                "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 주문 금액 계산 헬퍼 메서드
    private BigDecimal calculateTotalAmount(List<Cart> cartList, Map<Long, Product> productMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());

            BigDecimal itemTotalAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemTotalAmount);
        }

        return totalAmount;
    }

    /**
     * 보상 트랜잭션 실행 (롤백)
     * 예외 발생 시 이미 변경된 데이터를 원래 상태로 복구
     */
    private void compensateTransaction(RollbackContext rollbackContext) {
        // 1. User 포인트 잔액 복구
        if (rollbackContext.user != null && rollbackContext.usedPointAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                rollbackContext.user.refundPoint(rollbackContext.usedPointAmount);
                userRepository.save(rollbackContext.user);
            } catch (Exception e) {
                System.err.println("User 포인트 잔액 롤백 실패: " + e.getMessage());
            }
        }

        // 2. 포인트 복구 (역순으로 처리)
        for (Map.Entry<Point, BigDecimal> entry : rollbackContext.pointUsageMap.entrySet()) {
            try {
                Point point = entry.getKey();
                BigDecimal usedAmount = entry.getValue();

                // 사용된 포인트 복구 (usedAmount 감소)
                point.restoreUsedAmount(usedAmount);
                pointRepository.save(point);
            } catch (Exception e) {
                // 롤백 실패 시 로그만 남기고 계속 진행
                System.err.println("포인트 롤백 실패: " + e.getMessage());
            }
        }

        // 3. 쿠폰 복구
        if (rollbackContext.userCoupon != null && rollbackContext.coupon != null) {
            try {
                // UserCoupon 사용 횟수 복구
                rollbackContext.userCoupon.cancelUse(rollbackContext.coupon.getPerUserLimit());
                userCouponRepository.save(rollbackContext.userCoupon);

                // Coupon 사용 횟수 감소
                rollbackContext.coupon.decreaseUsageCount();
                couponRepository.save(rollbackContext.coupon);
            } catch (Exception e) {
                System.err.println("쿠폰 롤백 실패: " + e.getMessage());
            }
        }

        // 4. 상품 재고 복구
        for (Map.Entry<Long, Integer> entry : rollbackContext.productQuantityMap.entrySet()) {
            try {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                Product product = productRepository.findById(productId).orElse(null);
                if (product != null) {
                    // 재고 복구
                    product.increaseStock(quantity);
                    // 판매량 복구
                    product.decreaseSoldCount(quantity);
                    productRepository.save(product);
                }
            } catch (Exception e) {
                System.err.println("상품 재고 롤백 실패: " + e.getMessage());
            }
        }
    }
}
