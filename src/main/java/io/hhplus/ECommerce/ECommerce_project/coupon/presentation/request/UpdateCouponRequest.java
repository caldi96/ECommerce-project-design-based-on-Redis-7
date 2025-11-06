package io.hhplus.ECommerce.ECommerce_project.coupon.presentation.request;

import io.hhplus.ECommerce.ECommerce_project.coupon.application.command.UpdateCouponCommand;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateCouponRequest(

        @NotBlank(message = "쿠폰명은 필수입니다")
        String name,

        String code,  // 선택값

        @NotNull(message = "할인 타입은 필수입니다")
        DiscountType discountType,

        @NotNull(message = "할인 값은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "할인 값은 0보다 커야 합니다")
        BigDecimal discountValue,

        @DecimalMin(value = "0.0", inclusive = false, message = "최대 할인 금액은 0보다 커야 합니다")
        BigDecimal maxDiscountAmount,  // 선택값 (정률일 때)

        @DecimalMin(value = "0.0", message = "최소 주문 금액은 0 이상이어야 합니다")
        BigDecimal minOrderAmount,  // 선택값

        @NotNull(message = "총 수량은 필수입니다")
        @Min(value = 1, message = "총 수량은 1 이상이어야 합니다")
        Integer totalQuantity,

        @NotNull(message = "사용자당 제한은 필수입니다")
        @Min(value = 1, message = "사용자당 제한은 1 이상이어야 합니다")
        Integer perUserLimit,

        @NotNull(message = "시작일은 필수입니다")
        LocalDateTime startDate,

        @NotNull(message = "종료일은 필수입니다")
        LocalDateTime endDate
) {

    public UpdateCouponCommand toCommand(Long id) {
        return new UpdateCouponCommand(
                id,
                name,
                code,
                discountType,
                discountValue,
                maxDiscountAmount,
                minOrderAmount,
                totalQuantity,
                perUserLimit,
                startDate,
                endDate
        );
    }
}