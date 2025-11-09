package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.repository.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;
    private final UserRepository userRepository;
    private final Map<Long, Object> orderLockMap = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 주문 ID별 락 객체 획득
     */
    private Object getOrderLock(Long orderId) {
        return orderLockMap.computeIfAbsent(orderId, k -> new Object());
    }

    @Transactional
    public CreatePaymentResponse execute(CreatePaymentCommand command) {
        // 주문 ID별 락을 걸어서 동시성 제어
        synchronized (getOrderLock(command.orderId())) {
            // 1. 주문 조회
            Orders order = orderRepository.findById(command.orderId())
                    .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

            // 2. 주문이 결제 가능한 상태인지 확인 (PENDING 상태만 결제 가능)
            if (!order.isPending()) {
                throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_PAYMENT,
                    "결제 대기 중인 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
            }

            // 3. 결제 정보 생성
            Payment payment = Payment.createPayment(
                    order.getId(),
                    order.getFinalAmount(),
                    command.paymentMethod()
            );

            // 4. 결제 처리 (실제로는 외부 결제 API 호출)
            // TODO: 실제 결제 API 연동 시 이 부분 구현
            try {
                // 외부 결제 API 호출 시뮬레이션
                // boolean paymentSuccess = externalPaymentAPI.process(payment);

                // 현재는 항상 성공으로 처리 (테스트용)
                payment.complete();
                Payment savedPayment = paymentRepository.save(payment);

                // 5. 주문 상태를 PAID로 변경
                order.paid();
                orderRepository.save(order);

                return CreatePaymentResponse.from(savedPayment, order);

            } catch (Exception e) {
                // 결제 실패 처리
                payment.fail(e.getMessage());
                paymentRepository.save(payment);

                // 주문 상태를 PAYMENT_FAILED로 변경
                order.paymentFailed();
                orderRepository.save(order);

                // ✅ Saga 패턴: 주문 생성 시 차감한 리소스 복구 (보상 트랜잭션)
                rollbackOrderResources(order);

                // 예외를 다시 던져서 트랜잭션이 롤백되도록 함
                throw new PaymentException(ErrorCode.PAYMENT_ALREADY_FAILED,
                    "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    /**
     * 주문 생성 시 차감한 리소스를 복구하는 보상 트랜잭션 (Saga Pattern)
     * 결제 실패 시 재고, 쿠폰, 포인트를 원래대로 복구
     */
    private void rollbackOrderResources(Orders order) {
        try {
            // 1. 주문 아이템 조회
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            // 2. 상품 재고 복구 (동시성 제어 적용)
            for (OrderItem orderItem : orderItems) {
                try {
                    // 락 안에서 재고 복구를 원자적으로 수행하여 동시성 문제 해결
                    Product product = productRepository.restoreStockWithLock(
                            orderItem.getProductId(),
                            orderItem.getQuantity()
                    );

                    if (product == null) {
                        System.err.println("상품 재고 복구 실패: 상품을 찾을 수 없습니다 (Product ID: " + orderItem.getProductId() + ")");
                    }
                } catch (Exception e) {
                    System.err.println("상품 재고 복구 실패 (Product ID: " + orderItem.getProductId() + "): " + e.getMessage());
                }
            }

            // 3. 쿠폰 복구
            if (order.getCouponId() != null) {
                try {
                    // 사용자 쿠폰 조회
                    UserCoupon userCoupon = userCouponRepository
                            .findByUserIdAndCouponId(order.getUserId(), order.getCouponId())
                            .orElse(null);

                    // 쿠폰 정보 조회
                    Coupon coupon = couponRepository.findById(order.getCouponId())
                            .orElse(null);

                    if (userCoupon != null && coupon != null) {
                        // 쿠폰 사용 취소 처리
                        userCoupon.cancelUse(coupon.getPerUserLimit());
                        userCouponRepository.save(userCoupon);

                        // 쿠폰 사용 횟수 감소
                        coupon.decreaseUsageCount();
                        couponRepository.save(coupon);
                    }
                } catch (Exception e) {
                    System.err.println("쿠폰 복구 실패 (Coupon ID: " + order.getCouponId() + "): " + e.getMessage());
                }
            }

            // 4. 포인트 복구
            try {
                List<PointUsageHistory> pointUsageHistories =
                        pointUsageHistoryRepository.findByOrderIdAndCanceledAtIsNull(order.getId());

                BigDecimal totalRestoredPoint = BigDecimal.ZERO;

                for (PointUsageHistory history : pointUsageHistories) {
                    try {
                        // 원본 포인트 조회
                        Point originalPoint = pointRepository.findById(history.getPointId())
                                .orElse(null);

                        if (originalPoint != null) {
                            // 사용한 포인트 금액만큼 복구
                            originalPoint.restoreUsedAmount(history.getUsedAmount());
                            pointRepository.save(originalPoint);

                            // PointUsageHistory 취소 처리
                            history.cancel();
                            pointUsageHistoryRepository.save(history);

                            // 복구할 총 포인트 금액 누적
                            totalRestoredPoint = totalRestoredPoint.add(history.getUsedAmount());
                        }
                    } catch (Exception e) {
                        System.err.println("포인트 복구 실패 (Point ID: " + history.getPointId() + "): " + e.getMessage());
                    }
                }

                // User의 포인트 잔액 복구
                if (totalRestoredPoint.compareTo(BigDecimal.ZERO) > 0) {
                    User user = userRepository.findById(order.getUserId())
                            .orElse(null);

                    if (user != null) {
                        user.refundPoint(totalRestoredPoint);
                        userRepository.save(user);
                    }
                }
            } catch (Exception e) {
                System.err.println("포인트 복구 실패 (Order ID: " + order.getId() + "): " + e.getMessage());
            }

        } catch (Exception e) {
            // 보상 트랜잭션 전체 실패 시 로그만 남기고 계속 진행
            System.err.println("보상 트랜잭션 실패 (Order ID: " + order.getId() + "): " + e.getMessage());
        }
    }
}