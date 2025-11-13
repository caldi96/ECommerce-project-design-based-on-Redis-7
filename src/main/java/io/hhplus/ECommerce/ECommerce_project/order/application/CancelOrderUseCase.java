package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CancelOrderCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
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
        Orders order = orderRepository.findByIdWithLock(command.orderId())
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 소유자 확인
        if (!order.getUser().getId().equals(command.userId())) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND,
                    "해당 주문에 대한 권한이 없습니다.");
        }

        // 3. 주문 취소 가능 여부 확인 (PENDING, PAID, PAYMENT_FAILED 상태만 취소 가능)
        if (!order.canCancel() && !order.canCancelAfterPaid()) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_CANCEL,
                    "취소할 수 없는 주문 상태입니다. 현재 상태: " + order.getStatus());
        }

        // 4. 주문 아이템 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrders_Id(command.orderId());

        // 5. 상품 재고 복구 (동시성 제어 적용, 여러 사람이 동시에 주문 취소시 재고 복구에 동시성 이슈 발생 가능)
        for (OrderItem orderItem : orderItems) {
            // 5-1. 주문 아이템의 상품 조회(비관적 락)
            Product product = productRepository.findByIdWithLock(orderItem.getProduct().getId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 5-2. 해당 상품 재고 증가(복구)
            product.increaseStock(orderItem.getQuantity());
        }

        // 6. 쿠폰 복구
        if (order.getCoupon() != null) {
            // 6-1. 사용자 쿠폰 조회(비관적락)
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_IdWithLock(order.getUser().getId(), order.getCoupon().getId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(order.getCoupon().getId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 6-3. 쿠폰 사용 취소 처리 (usedCount 감소)
            // issuedQuantity는 복구하지 않음 (한번 발급되면 영구적)
            userCoupon.cancelUse(coupon.getPerUserLimit());
        }

        // 7. 포인트 복구 (PointUsageHistory 활용)
        List<PointUsageHistory> pointUsageHistories =
                pointUsageHistoryRepository.findByOrders_IdAndCanceledAtIsNull(command.orderId());

        BigDecimal totalRestoredPoint = BigDecimal.ZERO;

        for (PointUsageHistory history : pointUsageHistories) {
            // 7-1. 원본 포인트 조회
            Point originalPoint = pointRepository.findByIdWithLock(history.getPoint().getId())
                    .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

            // 7-2. 사용한 포인트 금액만큼 복구
            originalPoint.restoreUsedAmount(history.getUsedAmount());

            // 7-3. PointUsageHistory 취소 처리
            history.cancel();

            // 7-4. 복구할 총 포인트 금액 누적
            totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
        }

        // 7-5. User의 포인트 잔액 복구
        if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
            User user = userRepository.findByIdWithLock(command.userId())
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

            user.refundPoint(totalRestoredPoint);
        }

        // 8-1. 주문 아이템 상태 변경
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getStatus() == OrderItemStatus.ORDER_PENDING) {
                orderItem.cancel();
            } else if (orderItem.getStatus() == OrderItemStatus.ORDER_COMPLETED) {
                orderItem.cancelAfterComplete();
            }
        }

        // 8-2. 주문 상태 변경
        if (order.isPending()) {
            order.cancel();  // PENDING -> CANCELED (결제 전 주문 취소)
        } else if (order.isPaid()) {
            order.cancelAfterPaid();  // PAID -> CANCELED (결제 후 환불)
        } else if (order.isPaymentFailed()) {
            order.cancel();  // PAYMENT_FAILED -> CANCELED (결제 실패 후 취소)
        }
    }
}