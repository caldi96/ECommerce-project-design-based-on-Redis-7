package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
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
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.infrastructure.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateOrderFromProductUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;

    public CreateOrderResponse execute(CreateOrderFromProductCommand command) {

        // 1. 검증 및 사전 계산 (트랜잭션 밖)
        ValidatedOrderFromProductData validatedOrderFromProductData = validateAndCalculate((command));

        // 2. 핵심 쓰기 작업만 트랜잭션 처리 (트랜잭션 안)
        return executeCore(command, validatedOrderFromProductData);
    }

    private ValidatedOrderFromProductData validateAndCalculate(CreateOrderFromProductCommand command) {

        // 1. 사용자 확인 (락 없이)
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 수량 검증
        if (command.quantity() == null || command.quantity() <= 0) {
            throw new OrderException(ErrorCode.PRODUCT_QUANTITY_INVALID);
        }

        // 3. 상품 조회 및 검증 (락 없이)
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 주문 가능 여부 검증 (비활성/재고/최소/최대 주문량 체크)
        product.validateOrder(command.quantity());

        // 5. 주문 금액 계산
        BigDecimal totalAmount = product.getPrice()
                .multiply(BigDecimal.valueOf(command.quantity()));

        // 6. 배송비 계산
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 7. 쿠폰 사전 검증 (락 없이)
        BigDecimal discountAmount = BigDecimal.ZERO;

        Coupon coupon = null;

        if (command.couponId() != null) {
            // 7-1. 사용자 쿠폰 조회 (락 없음)
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 7-2. 쿠폰 조회 및 검증
            coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 7-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 7-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 7-5. 할인 금액 계산 (최소 주문 금액 검증 포함)
            discountAmount = coupon.calculateDiscountAmount(totalAmount);
        }

        // 8. 포인트 사전 검증
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

        // 검증된 데이터 반환
        return new ValidatedOrderFromProductData(totalAmount, shippingFee, discountAmount);
    }

    @Transactional
    private CreateOrderResponse executeCore(
            CreateOrderFromProductCommand command,
            ValidatedOrderFromProductData validatedOrderFromProductData
    ) {

        // 1. 사용자 확인 (락 걸기)
        User user = userRepository.findByIdWithLock(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 상품 조회 및 검증 (비관적 락 적용 - 원자적 처리)
        // 상품 행에 비관적 락을 걸어 다른 사용자의 접근을 제한함
        Product product = productRepository.findByIdWithLock(command.productId())
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 3. 주문 가능 여부 재검증
        product.validateOrder(command.quantity());

        // 4. 재고 차감 및 판매량 증가
        product.decreaseStock(command.quantity());
        product.increaseSoldCount(command.quantity());

        // 5. 쿠폰 사용 처리
        BigDecimal discountAmount = BigDecimal.ZERO;
        // 핵심, 검증
        Coupon coupon = null;

        if (command.couponId() != null) {
            // 5-1. 사용자 쿠폰 조회 (락 걸기)
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_IdWithLock(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 5-2. 쿠폰 조회 및 검증
            coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 5-3. 쿠폰 유효성 검증 (활성화, 기간 등) (재검증)
            coupon.validateAvailability();

            // 5-4. 사용자 쿠폰 사용 가능 여부 확인 (재검증)
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 5-5. 할인 금액 계산 (최소 주문 금액 검증 포함) - 핵심, 검증
            discountAmount = validatedOrderFromProductData.discountAmount();

            // 5-6. 쿠폰 사용 처리 (usedCount 증가) 핵심
            userCoupon.use(coupon.getPerUserLimit());
        }

        // 6. 포인트 사용 (포인트 사용시에만)
        BigDecimal pointAmount = BigDecimal.ZERO;
        List<Point> pointsToUpdate = new ArrayList<>(); // 사용될 포인트들
        List<BigDecimal> pointUsageAmounts = new ArrayList<>(); // 각 포인트에서 사용할 금액

        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {

            // 사용 가능한 포인트 조회
            List<Point> availablePoints = pointRepository.findAvailablePointsByUserId(command.userId());

            // 포인트 사용 처리 (선입선출)
            BigDecimal remainingPointToUse = command.pointAmount();
            for (Point point : availablePoints) {
                if (remainingPointToUse.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // 락을 걸어서 다시 조회
                Point lockedPoint = pointRepository.findByIdWithLock(point.getId())
                        .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

                // 해당 포인트에서 사용할 수 있는 금액 계산
                BigDecimal availableAmount = lockedPoint.getRemainingAmount();
                BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

                // 나중에 사용 이력 생성을 위해 임시 저장
                pointUsageAmounts.add(pointToUse);
                pointsToUpdate.add(lockedPoint);

                remainingPointToUse = remainingPointToUse.subtract(pointToUse);
            }

            pointAmount = command.pointAmount();
        }

        // 7. Order 생성 (최종 금액은 Order에서 create할때 계산)
        Orders order = Orders.createOrder(
                user,
                coupon,
                validatedOrderFromProductData.totalAmount(),
                validatedOrderFromProductData.shippingFee(),
                discountAmount,
                pointAmount
        );

        // 8. 저장
        Orders savedOrder = orderRepository.save(order);

        // 9. 포인트 사용 이력 저장 (Order ID가 필요하므로 주문 생성 후 처리)
        if (!pointUsageAmounts.isEmpty()) {
            for (int i = 0; i < pointUsageAmounts.size(); i++) {
                BigDecimal usageAmount = pointUsageAmounts.get(i);

                // 9-1. 기존 CHARGE/REFUND 포인트는 부분 사용 처리
                Point originalPoint = pointsToUpdate.get(i);
                originalPoint.usePartially(usageAmount);

                // 9-2. PointUsageHistory 생성 (주문과 포인트 연결 추적용)
                PointUsageHistory history = PointUsageHistory.create(
                        originalPoint,
                        savedOrder,
                        usageAmount
                );
                // 9-3. 포인트 사용 내역 저장
                pointUsageHistoryRepository.save(history);
            }

            if (pointAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 9-4. User의 포인트 잔액 차감
                user.usePoint(pointAmount);
            }
        }

        // 10. OrderItem 생성
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
}

// 검증된 데이터를 전달하기 위한 내부 DTO
record ValidatedOrderFromProductData(
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount
) {}