package io.hhplus.ECommerce.ECommerce_project.payment.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PaymentException;
import io.hhplus.ECommerce.ECommerce_project.order.application.service.OrderFinderService;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.service.OrderDomainService;
import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.application.service.PaymentCompensationService;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.entity.Payment;
import io.hhplus.ECommerce.ECommerce_project.payment.infrastructure.PaymentRepository;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderDomainService orderDomainService;
    private final OrderFinderService orderFinderService;
    private final PaymentCompensationService paymentCompensationService;

    @Transactional
    public CreatePaymentResponse execute(CreatePaymentCommand command) {

        // ID 검증
        orderDomainService.validateId(command.orderId());

        // 1. 주문 조회
        Orders order = orderFinderService.getOrderWithLock(command.orderId());

        // 2. 주문이 결제 가능한 상태인지 확인 (PENDING 상태만 결제 가능)
        orderDomainService.validateCanPayment(order);

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
            paymentCompensationService.compensate(order);

            // 예외를 다시 던져서 트랜잭션이 롤백되도록 함
            throw new PaymentException(ErrorCode.PAYMENT_ALREADY_FAILED,
                "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}