package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.infrastructure.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
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
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreatePaymentResponse execute(CreatePaymentCommand command) {
            // 1. 주문 조회
            Orders order = orderRepository.findByIdWithLock(command.orderId())
                    .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

            // 2. 주문이 결제 가능한 상태인지 확인 (PENDING 상태만 결제 가능)
            if (!order.isPending()) {
                throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_PAYMENT,
                    "결제 대기 중인 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
            }

            // 3. 결제 정보 생성
            Payment payment = Payment.createPayment(
                    order,
                    order.getFinalAmount(),
                    command.paymentMethod()
            );

            // 4. 결제 처리 (실제로는 외부 결제 API 호출)
            // TODO: 실제 결제 API 연동 시 이 부분 구현
            try {
                // 외부 결제 API 호출 시뮬레이션
                // boolean paymentSuccess = externalPaymentAPI.process(payment);

                // 현재는 항상 성공으로 처리 (테스트용, 추후 외부 결제 API 호출)
                payment.complete();
                Payment savedPayment = paymentRepository.save(payment);

                // 5. 주문 상태를 PAID로 변경
                order.paid();

                return CreatePaymentResponse.from(savedPayment, order);

            } catch (Exception e) {
                // 결제 실패 처리
                payment.fail(e.getMessage());

                // 주문 상태를 PAYMENT_FAILED로 변경
                order.paymentFailed();

                // Saga 패턴: 주문 생성 시 차감한 리소스 복구 (보상 트랜잭션)
                rollbackOrderResources(order);

                // 예외를 다시 던져서 트랜잭션이 롤백되도록 함
                throw new PaymentException(ErrorCode.PAYMENT_ALREADY_FAILED,
                    "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
    }

    /**
     * 주문 생성 시 차감한 리소스를 복구하는 보상 트랜잭션 (Saga Pattern)
     * 결제 실패 시 재고, 쿠폰, 포인트를 원래대로 복구
     */
    private void rollbackOrderResources(Orders order) {
        try {
            // 1. 주문 아이템 조회
            List<OrderItem> orderItems = orderItemRepository.findByOrders_Id(order.getId());

            // 2. 상품 재고 복구 (동시성 제어 적용)
            for (OrderItem orderItem : orderItems) {
                try {
                    // 락 안에서 재고 복구를 원자적으로 수행하여 동시성 문제 해결
                    Product product = productRepository.findByIdWithLock(orderItem.getProduct().getId())
                            .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

                    // 재고 및 판매량 복구
                    product.increaseStock(orderItem.getQuantity());
                    product.decreaseSoldCount(orderItem.getQuantity());
                } catch (Exception e) {
                    throw new ProductException(ErrorCode.PRODUCT_RESTORE_FAILED,
                            "상품 재고 복구 실패 (Product ID: " + orderItem.getProduct().getId() + "): " + e.getMessage());
                }
            }

            // 3. 쿠폰 복구
            if (order.getCoupon() != null && order.getCoupon().getId() != null) {
                try {
                    // 사용자 쿠폰 조회 (비관적 락 적용)
                    UserCoupon userCoupon = userCouponRepository
                            .findByUser_IdAndCoupon_IdWithLock(order.getUser().getId(), order.getCoupon().getId())
                            .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

                    // 쿠폰 사용 취소 처리 (usedCount 감소)
                    // issuedQuantity는 복구하지 않음 (한번 발급되면 영구적)
                    userCoupon.cancelUse(order.getCoupon().getPerUserLimit());
                    // JPA 변경 감지로 자동 저장
                } catch (Exception e) {
                    throw new CouponException(ErrorCode.COUPON_NOT_AVAILABLE,
                            "쿠폰 복구 실패 (Coupon ID: " + order.getCoupon().getId() + "): " + e.getMessage());
                }
            }

            // 4. 포인트 복구
            try {
                List<PointUsageHistory> pointUsageHistories =
                        pointUsageHistoryRepository.findByOrders_IdAndCanceledAtIsNull(order.getId());

                BigDecimal totalRestoredPoint = BigDecimal.ZERO;

                for (PointUsageHistory history : pointUsageHistories) {
                    try {
                        // 원본 포인트 조회
                        Point originalPoint = pointRepository.findByIdWithLock(history.getPoint().getId())
                                .orElseThrow(() -> new PointException(ErrorCode.POINT_NOT_FOUND));

                        // 사용한 포인트 금액만큼 복구
                        originalPoint.restoreUsedAmount(history.getUsedAmount());

                        // PointUsageHistory 취소 처리
                        history.cancel();

                        // 복구할 총 포인트 금액 누적
                        totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
                    } catch (Exception e) {
                        throw new PointException(ErrorCode.POINT_RESTORE_FAILED
                                , " (Point ID: " + history.getPoint().getId() + "): " + e.getMessage());
                    }
                }

                // User의 포인트 잔액 복구
                if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
                    User user = userRepository.findByIdWithLock(order.getUser().getId())
                            .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

                    user.refundPoint(totalRestoredPoint);
                }
            } catch (Exception e) {
                throw new UserException(ErrorCode.USER_POINT_RESTORE_FAILED
                        , " (Order ID: " + order.getId() + "): " + e.getMessage());
            }

        } catch (Exception e) {
            // 보상 트랜잭션 전체 실패 시 로그만 남기고 계속 진행
            throw new PaymentException(ErrorCode.PAYMENT_COMPENSATION_TRANSACTION_FAILED
                    , e.getMessage());
        }
    }
}