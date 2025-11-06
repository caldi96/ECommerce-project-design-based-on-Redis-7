package io.hhplus.ECommerce.ECommerce_project.coupon.application.command;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponCommand(
    String name,                        // 쿠폰 이름
    String code,                        // 쿠폰 코드 (고유값)
    DiscountType discountType,          // 할인 타입 (PERCENTAGE, FIXED)
    BigDecimal discountValue,           // 할인 값 (10% 또는 1000원)
    BigDecimal maxDiscountAmount,       // 최대 할인 금액 (정률일 때)
    BigDecimal minOrderAmount,          // 최소 주문 금액
    Integer totalQuantity,              // 총 발급 가능 수량
    Integer perUserLimit,               // 사용자당 최대 발급/사용 가능 횟수
    LocalDateTime startDate,            // 사용 시작일
    LocalDateTime endDate               // 사용 종료일
) {}
