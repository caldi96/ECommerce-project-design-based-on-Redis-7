package io.hhplus.ECommerce.ECommerce_project.payment.presentation;

import io.hhplus.ECommerce.ECommerce_project.payment.application.CreatePaymentUseCase;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.request.CreatePaymentRequest;
import io.hhplus.ECommerce.ECommerce_project.payment.presentation.response.CreatePaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    /**
     * 결제 생성
     * 주문이 완료된 후 결제를 진행합니다.
     */
    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResponse response = createPaymentUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}