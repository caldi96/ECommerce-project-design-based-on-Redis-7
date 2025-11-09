package io.hhplus.ECommerce.ECommerce_project.payment.application.command;

import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;

public record CreatePaymentCommand(
    Long orderId,
    PaymentMethod paymentMethod
) {
    public static CreatePaymentCommand of(Long orderId, PaymentMethod paymentMethod) {
        return new CreatePaymentCommand(orderId, paymentMethod);
    }
}