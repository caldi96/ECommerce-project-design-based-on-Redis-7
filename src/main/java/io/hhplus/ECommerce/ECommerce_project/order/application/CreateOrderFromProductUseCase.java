package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponUsageService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromProductData;
import io.hhplus.ECommerce.ECommerce_project.order.domain.constants.ShippingPolicy;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointUsageService;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.service.PointDomainService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.StockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.service.ProductDomainService;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateOrderFromProductUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockService stockService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final CouponFinderService couponFinderService;
    private final UserCouponFinderService userCouponFinderService;
    private final UserCouponUsageService userCouponUsageService;
    private final PointDomainService pointDomainService;
    private final PointFinderService pointFinderService;
    private final PointUsageService pointUsageService;

    public CreateOrderResponse execute(CreateOrderFromProductCommand command) {

        // 1. 검증 및 사전 계산 (트랜잭션 밖)
        ValidatedOrderFromProductData validatedOrderFromProductData = validateAndCalculate((command));

        // 2. 재고 차감 (트랜잭션 1)
        stockService.reserveStock(command.productId(), command.quantity());

        try {
            // 3. 주문 완료 (트랜잭션 2)
            return completeOrder(command, validatedOrderFromProductData);
        } catch (Exception e) {
            // 4. 실패 시 재고 복구 (보상 트랜잭션)
            stockService.compensateStock(command.productId(), command.quantity());
            throw e;
        }
    }

    private ValidatedOrderFromProductData validateAndCalculate(CreateOrderFromProductCommand command) {

        // 1. ID 검증
        userDomainService.validateId(command.userId());

        // 1. 사용자 확인 (락 없이)
        User user = userFinderService.getUser(command.userId());

        // 2. 상품 도메인 검증
        productDomainService.validateId(command.productId());
        productDomainService.validateQuantity(command.quantity());

        // 3. 상품 조회 및 검증 (락 없이)
        Product product = productFinderService.getProduct(command.productId());

        // 주문 가능 여부 검증 (비활성/재고/최소/최대 주문량 체크)
        product.validateOrder(command.quantity());

        // 4. 주문 금액 계산
        BigDecimal totalAmount = product.getPrice()
                .multiply(BigDecimal.valueOf(command.quantity()));

        // 5. 배송비 계산
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 6. 쿠폰 사전 검증 (락 없이)
        BigDecimal discountAmount = BigDecimal.ZERO;

        Coupon coupon = null;

        if (command.couponId() != null) {
            // 7-1. 사용자 쿠폰 조회 (락 없음)
            UserCoupon userCoupon = userCouponFinderService
                    .getUserCouponByUserIdAndCouponId(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 7-2. 쿠폰 조회 및 검증
            coupon = couponFinderService.getCoupon(command.couponId());

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
            List<Point> availablePoints = pointFinderService.getAvailablePoints(command.userId());

            // 사용 가능한 포인트 합계 계산 (남은 금액 기준)
            BigDecimal totalAvailablePoint = availablePoints.stream()
                    .map(Point::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 포인트 잔액 검증
            pointDomainService.validateAvailablePoint(totalAvailablePoint, command.pointAmount());
        }

        // 검증된 데이터 반환
        return new ValidatedOrderFromProductData(totalAmount, shippingFee, discountAmount);
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public CreateOrderResponse completeOrder(
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
}