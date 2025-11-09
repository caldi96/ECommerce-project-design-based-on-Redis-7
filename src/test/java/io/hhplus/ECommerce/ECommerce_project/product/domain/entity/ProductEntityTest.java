package io.hhplus.ECommerce.ECommerce_project.product.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductEntityTest {
    // ---------- 생성 테스트 ----------
    @Test
    @DisplayName("상품 생성 성공")
    void createProductSuccess() {
        Product p = Product.createProduct(
                "상품명",
                1L,
                "설명",
                BigDecimal.valueOf(1000),
                10,
                1,
                10
        );

        assertThat(p.getName()).isEqualTo("상품명");
        assertThat(p.getCategoryId()).isEqualTo(1L);
        assertThat(p.getStock()).isEqualTo(10);
        assertThat(p.isActive()).isTrue();
        assertThat(p.isSoldOutProduct()).isFalse();
    }

    @Test
    @DisplayName("상품명 null 이면 예외 발생")
    void createProductFail_nameNull() {
        assertThatThrownBy(() ->
                Product.createProduct(
                        null,
                        1L,
                        "설명",
                        BigDecimal.valueOf(1000),
                        10,
                        1,
                        10
                )
        ).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("가격이 0 미만이면 예외 발생")
    void createProductFail_negativePrice() {
        assertThatThrownBy(() ->
                Product.createProduct(
                        "상품",
                        1L,
                        "설명",
                        BigDecimal.valueOf(-1),
                        10,
                        1,
                        10
                )
        ).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("재고가 음수면 예외 발생")
    void createProductFail_negativeStock() {
        assertThatThrownBy(() ->
                Product.createProduct(
                        "상품",
                        1L,
                        "설명",
                        BigDecimal.valueOf(1000),
                        -1,
                        1,
                        10
                )
        ).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("최소 주문수량이 최대보다 크면 예외 발생")
    void createProductFail_minGreaterThanMax() {
        assertThatThrownBy(() ->
                Product.createProduct(
                        "상품",
                        1L,
                        "설명",
                        BigDecimal.valueOf(1000),
                        10,
                        5,
                        3
                )
        ).isInstanceOf(ProductException.class);
    }

    // ---------- 재고 ----------
    @Test
    @DisplayName("재고 감소 성공 + 품절 상태 자동 설정")
    void decreaseStockSuccess() {
        Product p = Product.createProduct("상품", 1L, "desc", BigDecimal.valueOf(1000), 5, 1, 10);
        p.decreaseStock(5);

        assertThat(p.getStock()).isEqualTo(0);
        assertThat(p.isSoldOutProduct()).isTrue();
    }

    @Test
    @DisplayName("재고 감소 시 재고 부족하면 예외")
    void decreaseStockFail_notEnough() {
        Product p = Product.createProduct("상품", 1L, "desc", BigDecimal.valueOf(1000), 3, 1, 10);

        assertThatThrownBy(() -> p.decreaseStock(5))
                .isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("재고 증가 시 품절 상태 해제")
    void increaseStockSuccess() {
        Product p = Product.createProduct("상품", 1L, "desc", BigDecimal.valueOf(1000), 0, 1, 10);
        p.increaseStock(5);

        assertThat(p.getStock()).isEqualTo(5);
        assertThat(p.isSoldOutProduct()).isFalse();
    }

    @Test
    @DisplayName("stock 직접 설정 성공")
    void updateStockSuccess() {
        Product p = Product.createProduct("상품", 1L, "desc", BigDecimal.valueOf(1000), 5, 1, 10);
        p.updateStock(0);

        assertThat(p.getStock()).isEqualTo(0);
        assertThat(p.isSoldOutProduct()).isTrue();
    }

    // ---------- 판매량 ----------
    @Test
    @DisplayName("판매량 증가 성공")
    void increaseSoldCountSuccess() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);
        p.increaseSoldCount(3);

        assertThat(p.getSoldCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("판매량 감소 시 현재보다 큰 경우 예외")
    void decreaseSoldCountFail_invalid() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);
        p.increaseSoldCount(2);

        assertThatThrownBy(() -> p.decreaseSoldCount(3))
                .isInstanceOf(ProductException.class);
    }

    // ---------- 활성/비활성 ----------
    @Test
    @DisplayName("상품 비활성화/활성화 성공")
    void activateDeactivate() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);

        p.deactivate();
        assertThat(p.isActive()).isFalse();

        p.activate();
        assertThat(p.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 활성 상태에서 activate() 호출 시 예외")
    void activateFail() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);

        assertThatThrownBy(p::activate)
                .isInstanceOf(ProductException.class);
    }

    // ---------- 품절 ----------
    @Test
    @DisplayName("수동 품절 처리")
    void outOfStockSuccess() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);

        p.outOfStock();
        assertThat(p.isSoldOutProduct()).isTrue();
    }

    @Test
    @DisplayName("품절 해제 시 재고가 0이면 예외")
    void backInStockFail_zeroStock() {
        // given
        Product product = Product.createProduct(
                "상품명", 1L, "설명", new BigDecimal("1000"), 0, null, null
        );

        // when & then
        assertThrows(ProductException.class, () -> product.backInStock());
    }

    // ---------- 삭제 ----------
    @Test
    @DisplayName("상품 삭제 성공")
    void deleteSuccess() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);

        p.delete();

        assertThat(p.getDeletedAt()).isNotNull();
        assertThat(p.isActive()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 상품 삭제 시 예외")
    void deleteFail() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);
        p.delete();

        assertThatThrownBy(p::delete)
                .isInstanceOf(ProductException.class);
    }

    // ---------- 주문 가능 여부 ----------
    @Test
    @DisplayName("주문 가능 조건 모두 충족 시 true")
    void canOrderSuccess() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 10);

        assertThat(p.canOrder(5)).isTrue();
    }

    @Test
    @DisplayName("재고 부족 시 주문 불가")
    void canOrderFail_stock() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 3, 1, 10);

        assertThat(p.canOrder(5)).isFalse();
    }

    @Test
    @DisplayName("최소 주문 수량 미달 시 false")
    void canOrderFail_min() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 3, 10);

        assertThat(p.canOrder(1)).isFalse();
    }

    @Test
    @DisplayName("최대 주문 수량 초과 시 false")
    void canOrderFail_max() {
        Product p = Product.createProduct("상품", 1L, "d", BigDecimal.valueOf(1000), 10, 1, 5);

        assertThat(p.canOrder(7)).isFalse();
    }
}
