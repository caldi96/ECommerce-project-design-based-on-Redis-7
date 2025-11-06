package io.hhplus.ECommerce.ECommerce_project.coupon.presentation.response;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CouponResponse(
        Long id,
        String name,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        int totalQuantity,
        int issuedQuantity,
        int usageCount,
        int perUserLimit,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getUsageCount(),
                coupon.getPerUserLimit(),
                coupon.getStartDate(),
                coupon.getEndDate(),
                coupon.isActive(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    public static List<CouponResponse> from(List<Coupon> couponList) {
        return couponList.stream()
                .map(CouponResponse::from)
                .toList();
    }
}
