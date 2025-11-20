package io.hhplus.ECommerce.ECommerce_project.product.domain.service;

import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ProductException;
import org.springframework.stereotype.Component;

@Component
public class ProductDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_ID_INVALID);
        }
    }

    /**
     * 수량 값이 유효한지 검증
     */
    public void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ProductException(ErrorCode.PRODUCT_QUANTITY_INVALID);
        }
    }
}
