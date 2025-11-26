package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.UserCouponFinderService;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CancelOrderCommand;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderItemFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import io.hhplus.ECommerce.ECommerce_project.order.domain.service.OrderDomainService;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointUsageHistoryFinderService;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.ProductFinderService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.RedisStockService;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderDomainService orderDomainService;
    private final OrderFinderService orderFinderService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;
    private final OrderItemFinderService orderItemFinderService;
    private final ProductFinderService productFinderService;
    private final CouponFinderService couponFinderService;
    private final UserCouponFinderService userCouponFinderService;
    private final PointFinderService pointFinderService;
    private final PointUsageHistoryFinderService pointUsageHistoryFinderService;
    private final RedisStockService redisStockService;

    @Transactional
    public void execute(CancelOrderCommand command) {

        // 1. ID 검증
        orderDomainService.validateId(command.orderId());
        userDomainService.validateId(command.userId());

        // 2. 유저 존재 유무 확인
        User user = userFinderService.getUser(command.userId());

        // 3. 주문 조회
        Orders order = orderFinderService.getOrderWithLock(command.orderId());

        // 4. 주문 소유자 확인
        orderDomainService.validateOrderOwner(order, user);

        // 5. 주문 취소 가능 여부 확인 (PENDING, PAID, PAYMENT_FAILED 상태만 취소 가능)
        orderDomainService.validateCancelable(order);

        // 6. 주문 아이템 조회
        List<OrderItem> orderItems = orderItemFinderService.getOrderItems(command.orderId());

        // 7. 상품 재고 복구 (동시성 제어 적용, 여러 사람이 동시에 주문 취소시 재고 복구에 동시성 이슈 발생 가능)
        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProduct().getId();

            // 7-1. 주문 아이템의 상품 조회(비관적 락)
            Product product = productFinderService.getProductWithLock(orderItem.getProduct().getId());

            // 7-2. 해당 상품 재고 증가(DB 복구)
            product.increaseStock(orderItem.getQuantity());

            // 7-3. Redis 재고 복구
            redisStockService.setStock(productId, product.getStock());
        }

        // 8. 쿠폰 복구
        if (order.getCoupon() != null) {
            // 8-1. 사용자 쿠폰 조회(비관적락)
            UserCoupon userCoupon = userCouponFinderService.getUserCouponWithLock(order.getUser(), order.getCoupon());

            // 8-2. 쿠폰 정보 조회
            Coupon coupon = couponFinderService.getCoupon(order.getCoupon().getId());

            // 8-3. 쿠폰 사용 취소 처리 (usedCount 감소)
            // issuedQuantity는 복구하지 않음 (한번 발급되면 영구적)
            userCoupon.cancelUse(coupon.getPerUserLimit());
        }

        // 9. 포인트 복구 (PointUsageHistory 활용)
        List<PointUsageHistory> pointUsageHistories =
                pointUsageHistoryFinderService.getPointUsageHistories(command.orderId());

        BigDecimal totalRestoredPoint = BigDecimal.ZERO;

        for (PointUsageHistory history : pointUsageHistories) {
            // 9-1. 원본 포인트 조회
            Point originalPoint = pointFinderService.getPointWithLock(history.getPoint().getId());

            // 9-2. 사용한 포인트 금액만큼 복구
            originalPoint.restoreUsedAmount(history.getUsedAmount());

            // 9-3. PointUsageHistory 취소 처리
            history.cancel();

            // 9-4. 복구할 총 포인트 금액 누적
            totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
        }

        // 9-5. User의 포인트 잔액 복구
        if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
            User lockedUser = userFinderService.getUserWithLock(command.userId());

            lockedUser.refundPoint(totalRestoredPoint);
        }

        // 10-1. 주문 아이템 상태 변경
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getStatus() == OrderItemStatus.ORDER_PENDING) {
                orderItem.cancel();
            } else if (orderItem.getStatus() == OrderItemStatus.ORDER_COMPLETED) {
                orderItem.cancelAfterComplete();
            }
        }

        // 10-2. 주문 상태 변경
        if (order.isPending()) {
            order.cancel();  // PENDING -> CANCELED (결제 전 주문 취소)
        } else if (order.isPaid()) {
            order.cancelAfterPaid();  // PAID -> CANCELED (결제 후 환불)
        } else if (order.isPaymentFailed()) {
            order.cancel();  // PAYMENT_FAILED -> CANCELED (결제 실패 후 취소)
        }
    }
}