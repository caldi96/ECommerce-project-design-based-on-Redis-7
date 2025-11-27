package io.hhplus.ECommerce.ECommerce_project.order.application.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import io.hhplus.ECommerce.ECommerce_project.coupon.application.service.CouponCompensationService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.point.application.service.PointCompensationService;
import io.hhplus.ECommerce.ECommerce_project.product.application.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompensationService {

    private final OrderItemFinderService orderItemFinderService;
    private final StockService stockService;
    private final CouponCompensationService couponCompensationService;
    private final PointCompensationService pointCompensationService;

    /**
     * 결제 실패 시 주문 생성 중 차감한 리소스를 복구하는 보상 트랜잭션 (Saga Pattern)
     * 각 보상 메서드에서 발생한 예외(ProductException, CouponException, UserException)를 그대로 전파
     */
    public void compensate(Orders order) {
        compensateStock(order); // 재고는 복구할때 비관적락 걸려있음
        compensateCoupon(order);
        compensatePoint(order);
    }

    /**
     * 재고 복구
     */
    private void compensateStock(Orders order) {
        try {
            // 1. 주문 아이템 조회
            List<OrderItem> orderItems = orderItemFinderService.getOrderItems(order.getId());

            // 2. 상품 재고 복구 (동시성 제어 적용)
            for (OrderItem orderItem : orderItems) {
                stockService.compensateStock(
                        orderItem.getProduct().getId(),
                        orderItem.getQuantity()
                );
            }
        } catch (Exception e) {
            throw new ProductException(
                    ErrorCode.PRODUCT_RESTORE_FAILED,
                    "상품 재고 복구 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 쿠폰 복구
     */
    private void compensateCoupon(Orders order) {
        if (order.getCoupon() == null || order.getCoupon().getId() == null) {
            return;
        }

        try {
            couponCompensationService.compensate(
                    order.getUser().getId(),
                    order.getCoupon().getId(),
                    order.getCoupon().getPerUserLimit()
            );
        } catch (Exception e) {
            throw new CouponException(
                    ErrorCode.COUPON_NOT_AVAILABLE,
                    "쿠폰 복구 실패 (Coupon ID: " + order.getCoupon().getId() + "): " + e.getMessage()
            );
        }
    }

    /**
     * 포인트 복구
     */
    private void compensatePoint(Orders order) {
        try {
            pointCompensationService.compensate(
                    order.getId(),
                    order.getUser().getId()
            );
        } catch (Exception e) {
            throw new UserException(
                    ErrorCode.USER_POINT_RESTORE_FAILED,
                    "포인트 복구 실패 (Order ID: " + order.getId() + "): " + e.getMessage()
            );

        }
    }
}
