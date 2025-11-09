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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void execute(CancelOrderCommand command) {
        // 1. 주문 조회
        Orders order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 소유자 확인
        if (!order.getUserId().equals(command.userId())) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND,
                    "해당 주문에 대한 권한이 없습니다.");
        }

        // 3. 주문 취소 가능 여부 확인 (PENDING 또는 PAID 상태만 취소 가능)
        if (!order.canCancel() && !order.canCancelAfterPaid()) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_CANCEL,
                    "취소할 수 없는 주문 상태입니다. 현재 상태: " + order.getStatus());
        }

        // 4. 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(command.orderId());

        // 5. 상품 재고 복구
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            product.increaseStock(orderItem.getQuantity());
            product.decreaseSoldCount(orderItem.getQuantity());
            productRepository.save(product);
        }

        // 6. 쿠폰 복구
        if (order.getCouponId() != null) {
            // 6-1. 사용자 쿠폰 조회
            UserCoupon userCoupon = userCouponRepository
                    .findByUserIdAndCouponId(order.getUserId(), order.getCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(order.getCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 6-3. 쿠폰 사용 취소 처리
            userCoupon.cancelUse(coupon.getPerUserLimit());
            userCouponRepository.save(userCoupon);

            // 6-4. 쿠폰 사용 횟수 감소
            coupon.decreaseUsageCount();
            couponRepository.save(coupon);
        }

        // 7. 포인트 복구 (PointUsageHistory 활용)
        List<PointUsageHistory> pointUsageHistories =
                pointUsageHistoryRepository.findByOrderIdAndCanceledAtIsNull(command.orderId());

        BigDecimal totalRestoredPoint = BigDecimal.ZERO;

        for (PointUsageHistory history : pointUsageHistories) {
            // 7-1. 원본 포인트 조회
            Point originalPoint = pointRepository.findById(history.getPointId())
                    .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

            // 7-2. 사용한 포인트 금액만큼 복구
            originalPoint.restoreUsedAmount(history.getUsedAmount());
            pointRepository.save(originalPoint);

            // 7-3. PointUsageHistory 취소 처리
            history.cancel();
            pointUsageHistoryRepository.save(history);

            // 7-4. 복구할 총 포인트 금액 누적
            totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
        }

        // 7-5. User의 포인트 잔액 복구
        if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
            User user = userRepository.findById(command.userId())
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

            user.refundPoint(totalRestoredPoint);
            userRepository.save(user);
        }

        // 8. 주문 상태 변경
        if (order.isPending()) {
            order.cancel();  // PENDING -> CANCELED
        } else if (order.isPaid()) {
            order.cancelAfterPaid();  // PAID -> CANCELED
        }

        orderRepository.save(order);
    }
}