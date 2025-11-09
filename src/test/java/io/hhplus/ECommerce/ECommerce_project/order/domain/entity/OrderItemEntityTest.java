package io.hhplus.ECommerce.ECommerce_project.order.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class OrderItemEntityTest {

    @Test
    void createOrderItem_success() {
        // given
        Long orderId = 1L;
        Long productId = 10L;
        String productName = "테스트 상품";
        int quantity = 2;
        BigDecimal unitPrice = BigDecimal.valueOf(5000);

        // when
        OrderItem item = OrderItem.createOrderItem(orderId, productId, productName, quantity, unitPrice);

        // then
        assertThat(item.getOrderId()).isEqualTo(orderId);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo(productName);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(item.getSubTotal()).isEqualTo(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDER_PENDING);
        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void complete_pendingItem_changesStatus() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        item.complete();

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDER_COMPLETED);
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void complete_nonPendingItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item.complete(); // 상태 완료로 변경

        assertThatThrownBy(item::complete)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("대기 중인 주문 항목만 완료 처리할 수 있습니다.");
    }

    @Test
    void cancel_pendingItem_changesStatus() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        item.cancel();

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDER_CANCELED);
        assertThat(item.getCanceledAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancel_nonPendingItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item.complete(); // 상태 완료로 변경

        assertThatThrownBy(item::cancel)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("대기 중인 주문 항목만 취소할 수 있습니다.");
    }

    @Test
    void cancelAfterComplete_completedItem_changesStatus() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item.complete();

        item.cancelAfterComplete();

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.ORDER_CANCELED);
        assertThat(item.getCanceledAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancelAfterComplete_nonCompletedItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        assertThatThrownBy(item::cancelAfterComplete)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("완료된 주문 항목만 취소할 수 있습니다.");
    }

    @Test
    void confirmPurchase_completedItem_changesStatus() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item.complete();

        item.confirmPurchase();

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PURCHASE_CONFIRMED);
        assertThat(item.getConfirmedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void confirmPurchase_nonCompletedItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        assertThatThrownBy(item::confirmPurchase)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("완료된 주문 항목만 구매 확정할 수 있습니다.");
    }

    @Test
    void returnItem_completedOrConfirmedItem_changesStatus() {
        OrderItem item1 = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item1.complete();
        item1.returnItem();
        assertThat(item1.getStatus()).isEqualTo(OrderItemStatus.ORDER_RETURNED);

        OrderItem item2 = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item2.complete();
        item2.confirmPurchase();
        item2.returnItem();
        assertThat(item2.getStatus()).isEqualTo(OrderItemStatus.ORDER_RETURNED);
    }

    @Test
    void returnItem_nonCompletedOrConfirmedItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        assertThatThrownBy(item::returnItem)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("완료 또는 구매 확정된 주문 항목만 반품할 수 있습니다.");
    }

    @Test
    void refund_canceledOrReturnedItem_changesStatus() {
        OrderItem item1 = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item1.cancel();
        item1.refund();
        assertThat(item1.getStatus()).isEqualTo(OrderItemStatus.ORDER_REFUNDED);

        OrderItem item2 = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));
        item2.complete();
        item2.returnItem();
        item2.refund();
        assertThat(item2.getStatus()).isEqualTo(OrderItemStatus.ORDER_REFUNDED);
    }

    @Test
    void refund_nonCanceledOrReturnedItem_throwsException() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 1, BigDecimal.valueOf(1000));

        assertThatThrownBy(item::refund)
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("취소 또는 반품된 주문 항목만 환불할 수 있습니다.");
    }

    @Test
    void recalculateSubTotal_updatesSubTotal() {
        OrderItem item = OrderItem.createOrderItem(1L, 10L, "상품", 2, BigDecimal.valueOf(1000));

        item.recalculateSubTotal();

        assertThat(item.getSubTotal()).isEqualTo(BigDecimal.valueOf(2000));
    }
}
