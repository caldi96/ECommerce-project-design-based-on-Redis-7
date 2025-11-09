package io.hhplus.ECommerce.ECommerce_project.payment.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.payment.application.command.CreatePaymentCommand;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
    @NotNull(message = "주문 ID는 필수입니다.")
    Long orderId,

    @NotNull(message = "결제 수단은 필수입니다.")
    PaymentMethod paymentMethod
) {
    public CreatePaymentCommand toCommand() {
        return CreatePaymentCommand.of(orderId, paymentMethod);
    }
}