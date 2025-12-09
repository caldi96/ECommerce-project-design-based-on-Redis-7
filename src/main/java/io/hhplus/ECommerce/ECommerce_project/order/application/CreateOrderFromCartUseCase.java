package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.service.CartFinderService;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.service.CartDomainService;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.service.CouponDomainService;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.dto.ValidatedOrderFromCartData;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderCompletionService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.constants.ShippingPolicy;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderResponse;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateOrderFromCartUseCase {

    private final StockService stockService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final CartDomainService cartDomainService;
    private final CartFinderService cartFinderService;
    private final ProductDomainService productDomainService;
    private final ProductFinderService productFinderService;
    private final CouponDomainService couponDomainService;
    private final CouponFinderService couponFinderService;
    private final UserCouponFinderService userCouponFinderService;
    private final PointDomainService pointDomainService;
    private final PointFinderService pointFinderService;
    private final OrderCompletionService orderCompletionService;

    public CreateOrderResponse execute(CreateOrderFromCartCommand command) {

        // 1. 검증 및 사전 계산 (트랜잭션 밖)
        ValidatedOrderFromCartData validatedOrderFromCartData = validateAndCalculate((command));

        // 2. 재고 차감 (트랜잭션 1)
        stockService.reserveStocks(validatedOrderFromCartData.sortedEntries());

        try {
            // 3. 주문 완료 (트랜잭션 2)
            return orderCompletionService.completeOrderFromCart(command, validatedOrderFromCartData);
        } catch (Exception e) {
            // 4. 실패 시 재고 복구
            stockService.compensateStocks(validatedOrderFromCartData.sortedEntries());
            throw e;
        }
    }

    private ValidatedOrderFromCartData validateAndCalculate(CreateOrderFromCartCommand command) {

        // 1. ID 검증
        userDomainService.validateId(command.userId());

        // 1. 사용자 확인
        User user = userFinderService.getUser(command.userId());

        // 2. 장바구니 아이템 조회 (cartItemIds)
        List<Cart> cartList = command.cartItemIds().stream()
                .map(cartId -> {

                    // cartId 검증
                    cartDomainService.validateId(cartId);

                    Cart cart = cartFinderService.getCart(cartId);

                    // 유저의 카트인지 확인
                    cartDomainService.validateSameUser(cart, user);

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

            // 상품 도메인 검증
            productDomainService.validateId(productId);
            productDomainService.validateQuantity(totalQuantity);

            // 상품 조회 및 검증 (락 없이)
            Product product = productFinderService.getProduct(productId);

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

            // 도메인 검증
            couponDomainService.validateId(command.couponId());

            // 6-1. 사용자 쿠폰 조회
            UserCoupon userCoupon = userCouponFinderService
                    .getUserCouponByUserIdAndCouponId(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 조회 및 검증
            coupon = couponFinderService.getCoupon(command.couponId());

            // 6-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 6-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 6-5. 할인 금액 계산 (최소 주문 금액 검증 포함)
            discountAmount = coupon.calculateDiscountAmount(totalAmount);
        }

        // 7. 포인트 잔액 검증 (사전 검증 - 재고 차감 전에 포인트 부족 감지)
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

        return new ValidatedOrderFromCartData(
                cartList,
                sortedEntries,
                productMap,
                totalAmount,
                shippingFee,
                discountAmount
        );
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
