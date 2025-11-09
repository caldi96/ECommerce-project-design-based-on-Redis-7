package io.hhplus.ECommerce.ECommerce_project.payment.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PaymentException;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentMethod;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentStatus;
import io.hhplus.ECommerce.ECommerce_project.payment.domain.enums.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentEntityTest {
    @Test
    void createPayment_Success() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);

        assertNotNull(payment);
        assertEquals(1L, payment.getOrderId());
        assertEquals(BigDecimal.valueOf(1000), payment.getAmount());
        assertEquals(PaymentType.PAYMENT, payment.getPaymentType());
        assertEquals(PaymentMethod.CARD, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
    }

    @Test
    void createPayment_InvalidAmount_ThrowsException() {
        PaymentException exception = assertThrows(PaymentException.class,
                () -> Payment.createPayment(1L, BigDecimal.valueOf(-100), PaymentMethod.CARD));
        assertEquals(ErrorCode.PAYMENT_AMOUNT_INVALID, exception.getErrorCode());
    }

    @Test
    void createPayment_NullOrderId_ThrowsException() {
        PaymentException exception = assertThrows(PaymentException.class,
                () -> Payment.createPayment(null, BigDecimal.valueOf(1000), PaymentMethod.CARD));
        assertEquals(ErrorCode.PAYMENT_ORDER_ID_REQUIRED, exception.getErrorCode());
    }

    @Test
    void completePayment_Success() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        payment.complete();

        assertEquals(PaymentStatus.COMPLETED, payment.getPaymentStatus());
        assertNotNull(payment.getCompletedAt());
    }

    @Test
    void completePayment_AlreadyCompleted_ThrowsException() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        payment.complete();

        PaymentException exception = assertThrows(PaymentException.class, payment::complete);
        assertEquals(ErrorCode.PAYMENT_ALREADY_COMPLETED, exception.getErrorCode());
    }

    @Test
    void failPayment_Success() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        payment.fail("결제 실패");

        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("결제 실패", payment.getFailureReason());
        assertNotNull(payment.getFailedAt());
    }

    @Test
    void failPayment_InvalidReason_ThrowsException() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        PaymentException exception = assertThrows(PaymentException.class,
                () -> payment.fail("   "));

        assertEquals(ErrorCode.PAYMENT_FAILURE_REASON_REQUIRED, exception.getErrorCode());
    }

    @Test
    void refundPayment_Success() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        payment.complete();

        payment.refund();
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());
    }

    @Test
    void refundPayment_NotCompleted_ThrowsException() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        PaymentException exception = assertThrows(PaymentException.class, payment::refund);

        assertEquals(ErrorCode.PAYMENT_ONLY_COMPLETED_CAN_REFUND, exception.getErrorCode());
    }

    @Test
    void refundPayment_AlreadyRefunded_ThrowsException() {
        Payment payment = Payment.createPayment(1L, BigDecimal.valueOf(1000), PaymentMethod.CARD);
        payment.complete();
        payment.refund();

        PaymentException exception = assertThrows(PaymentException.class, payment::refund);
        assertEquals(ErrorCode.PAYMENT_ALREADY_REFUNDED, exception.getErrorCode());
    }

}
