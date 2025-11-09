package io.hhplus.ECommerce.ECommerce_project.coupon.application.command;

public record IssueCouponCommand(
    Long userId,
    Long couponId
) {}