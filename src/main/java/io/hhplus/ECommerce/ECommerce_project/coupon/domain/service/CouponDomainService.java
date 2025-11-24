package io.hhplus.ECommerce.ECommerce_project.coupon.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CouponDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new CouponException(ErrorCode.COUPON_ID_INVALID);
        }
    }
}
