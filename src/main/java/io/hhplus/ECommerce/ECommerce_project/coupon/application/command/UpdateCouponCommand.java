package io.hhplus.ECommerce.ECommerce_project.coupon.application.command;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateCouponCommand(
    Long id,
    String name,
    String code,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal maxDiscountAmount,
    BigDecimal minOrderAmount,
    Integer totalQuantity,
    Integer perUserLimit,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}