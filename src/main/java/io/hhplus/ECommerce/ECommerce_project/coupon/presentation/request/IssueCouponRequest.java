package io.hhplus.ECommerce.ECommerce_project.coupon.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.IssueCouponCommand;
import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(

        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @NotNull(message = "쿠폰 ID는 필수입니다")
        Long couponId
) {

    public IssueCouponCommand toCommand() {
        return new IssueCouponCommand(
                userId,
                couponId
        );
    }
}