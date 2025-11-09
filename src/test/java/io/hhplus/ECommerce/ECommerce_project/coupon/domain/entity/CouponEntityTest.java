package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CouponEntityTest {

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(1);
        endDate = LocalDateTime.now().plusDays(1);
    }

    @Test
    void createCoupon_success_percentage() {
        Coupon coupon = Coupon.createCoupon(
                "10% 할인쿠폰",
                "CODE10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("1000"),
                new BigDecimal("5000"),
                100,
                1,
                startDate,
                endDate
        );

        assertThat(coupon.getName()).isEqualTo("10% 할인쿠폰");
        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(coupon.getDiscountValue()).isEqualTo(new BigDecimal("10"));
        assertThat(coupon.getMaxDiscountAmount()).isEqualTo(new BigDecimal("1000"));
        assertThat(coupon.getMinOrderAmount()).isEqualTo(new BigDecimal("5000"));
        assertThat(coupon.getTotalQuantity()).isEqualTo(100);
        assertThat(coupon.getPerUserLimit()).isEqualTo(1);
        assertThat(coupon.isAvailableNow()).isTrue();
    }

    @Test
    void createCoupon_success_fixed() {
        Coupon coupon = Coupon.createCoupon(
                "5000원 할인",
                "FIX5000",
                DiscountType.FIXED,
                new BigDecimal("5000"),
                null,
                new BigDecimal("20000"),
                50,
                1,
                startDate,
                endDate
        );

        assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(coupon.getDiscountValue()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    void activate_deactivate_coupon() {
        Coupon coupon = Coupon.createCoupon(
                "테스트",
                "TEST",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                startDate,
                endDate
        );

        // 이미 활성 상태이므로 activate 시 예외
        assertThatThrownBy(coupon::activate)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_ALREADY_ACTIVE.getMessage());

        // 비활성화 후 다시 활성화
        coupon.deactivate();
        assertThat(coupon.isActive()).isFalse();

        coupon.activate();
        assertThat(coupon.isActive()).isTrue();
    }

    @Test
    void calculateDiscountAmount_percentage() {
        Coupon coupon = Coupon.createCoupon(
                "10% 할인",
                "PERCENT10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("500"),
                null,
                10,
                1,
                startDate,
                endDate
        );

        BigDecimal discount = coupon.calculateDiscountAmount(new BigDecimal("10000"));
        assertThat(discount).isEqualTo(new BigDecimal("500")); // 최대 할인 적용

        discount = coupon.calculateDiscountAmount(new BigDecimal("4000"));
        assertThat(discount).isEqualTo(new BigDecimal("400")); // 최대 할인 미적용
    }

    @Test
    void calculateDiscountAmount_fixed() {
        Coupon coupon = Coupon.createCoupon(
                "5000원 할인",
                "FIX5000",
                DiscountType.FIXED,
                new BigDecimal("5000"),
                null,
                null,
                10,
                1,
                startDate,
                endDate
        );

        BigDecimal discount = coupon.calculateDiscountAmount(new BigDecimal("10000"));
        assertThat(discount).isEqualTo(new BigDecimal("5000"));

        discount = coupon.calculateDiscountAmount(new BigDecimal("3000"));
        assertThat(discount).isEqualTo(new BigDecimal("3000")); // 주문금액보다 할인금액이 클 수 없음
    }

    @Test
    void increaseIssuedQuantity_exceedTotal_throwsException() {
        Coupon coupon = Coupon.createCoupon(
                "테스트",
                "TEST",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                1,
                1,
                startDate,
                endDate
        );

        coupon.increaseIssuedQuantity();

        assertThatThrownBy(coupon::increaseIssuedQuantity)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_ALL_ISSUED.getMessage());
    }

    @Test
    void validateAvailability_notStartedOrExpired() {
        Coupon futureCoupon = Coupon.createCoupon(
                "미래 쿠폰",
                "FUTURE",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        assertThat(futureCoupon.isAvailableNow()).isFalse();
        assertThatThrownBy(futureCoupon::validateAvailability)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_NOT_STARTED.getMessage());

        Coupon pastCoupon = Coupon.createCoupon(
                "지난 쿠폰",
                "PAST",
                DiscountType.FIXED,
                new BigDecimal("1000"),
                null,
                null,
                10,
                1,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        );

        assertThat(pastCoupon.isAvailableNow()).isFalse();
        assertThatThrownBy(pastCoupon::validateAvailability)
                .isInstanceOf(CouponException.class)
                .hasMessage(ErrorCode.COUPON_EXPIRED.getMessage());
    }
}
