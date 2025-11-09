package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PaymentException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.repository.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
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

            // 2. 주문이 결제 가능한 상태인지 확인 (COMPLETED 상태만 결제 가능)
            if (!order.isCompleted()) {
                throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_PAYMENT,
                    "주문 완료 상태만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
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

                // 예외를 다시 던져서 트랜잭션이 롤백되도록 함
                throw new PaymentException(ErrorCode.PAYMENT_ALREADY_FAILED,
                    "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }
}