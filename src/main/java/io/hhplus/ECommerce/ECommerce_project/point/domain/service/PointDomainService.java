package io.hhplus.ECommerce.ECommerce_project.point.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.PointException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.UserException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new PointException(ErrorCode.POINT_ID_INVALID);
        }
    }

    /**
     * 포인트 금액 검증
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PointException(ErrorCode.POINT_AMOUNT_INVALID);
        }
    }

    /**
     * 포인트 잔액 검증
     */
    public void validateAvailablePoint(BigDecimal userPoint, BigDecimal pointAmount) {
        if (userPoint.compareTo(pointAmount) < 0) {
            throw new PointException(ErrorCode.POINT_INSUFFICIENT_POINT);
        }
    }
}
