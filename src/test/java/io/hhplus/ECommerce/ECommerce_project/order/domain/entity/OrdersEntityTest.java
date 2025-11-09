package io.hhplus.ECommerce.ECommerce_project.order.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class OrdersEntityTest {

    @Test
    void createOrder_success() {
        // given
        Long userId = 1L;
        BigDecimal totalAmount = BigDecimal.valueOf(10000);
        BigDecimal shippingFee = BigDecimal.valueOf(2500);
        Long couponId = 10L;
        BigDecimal discountAmount = BigDecimal.valueOf(2000);
        BigDecimal pointAmount = BigDecimal.valueOf(500);
        List<Long> usedPointIds = List.of(1L, 2L);

        // when
        Orders order = Orders.createOrder(
                userId, totalAmount, shippingFee, couponId,
                discountAmount, pointAmount, usedPointIds
        );

        // then
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getCouponId()).isEqualTo(couponId);
        assertThat(order.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(order.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(order.getPointAmount()).isEqualTo(pointAmount);
        assertThat(order.getFinalAmount()).isEqualTo(totalAmount.add(shippingFee).subtract(discountAmount).subtract(pointAmount));
        assertThat(order.getShippingFee()).isEqualTo(shippingFee);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getUsedPointIds()).containsExactlyElementsOf(usedPointIds);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void createOrder_negativeFinalAmount_throwsException() {
        // given
        Long userId = 1L;

        // totalAmount + shippingFee < discount + point
        BigDecimal totalAmount = BigDecimal.valueOf(1000);
        BigDecimal shippingFee = BigDecimal.valueOf(500);
        BigDecimal discountAmount = BigDecimal.valueOf(1200);
        BigDecimal pointAmount = BigDecimal.valueOf(500);

        // then
        assertThatThrownBy(() -> Orders.createOrder(
                userId, totalAmount, shippingFee, null,
                discountAmount, pointAmount, null
        )).isInstanceOf(OrderException.class)
                .hasMessageContaining("최종 결제 금액은 0 이상이어야 합니다.");
    }

    @Test
    void paid_changesStatusAndSetsPaidAt() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);

        order.paid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void paid_invalidStatus_throwsException() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);
        order.cancel(); // COMPLETED -> CANCELED 상태 변경

        assertThatThrownBy(order::paid)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("주문 완료된 주문만 결제할 수 있습니다.");
    }

    @Test
    void cancel_completedOrder_changesStatus() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);

        // COMPLETED 상태에서 취소
        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancelAfterPaid_paidOrder_changesStatus() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);
        order.paid();

        order.cancelAfterPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void complete_paidOrder_changesStatus() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);
        order.paid();

        order.complete();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void paymentFailed_completedOrder_changesStatus() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(0),
                null, null, null, null);

        order.paymentFailed();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    void isFreeShipping_returnsCorrectly() {
        Orders order = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.ZERO,
                null, null, null, null);
        assertThat(order.isFreeShipping()).isTrue();

        Orders order2 = Orders.createOrder(1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                null, null, null, null);
        assertThat(order2.isFreeShipping()).isFalse();
    }
}
