package io.hhplus.ECommerce.ECommerce_project.cart.domain.service;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CartDomainService {

    /**
     * ID 값이 유효한지 검증
     */
    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new CartException(ErrorCode.CART_ID_INVALID);
        }
    }

    /**
     * 수량이 양수인지 검증
     */
    public void validateQuantityPositive(int quantity) {
        if (quantity <= 0) {
            throw new CartException(ErrorCode.CART_QUANTITY_INVALID);
        }
    }

    /**
     * 상품을 수량만큼 주문 가능한지 검증
     */
    public void validateCanOrder(Product product, int desiredQuantity) {
        if (!product.canOrder(desiredQuantity)) {
            throw new CartException(ErrorCode.CART_PRODUCT_CANNOT_BE_ADDED_TO_CART);
        }
    }

    /**
     * 해당 사용자의 장바구니인지 검증
     */
    public void validateSameUser(Cart cart, User user) {
        if (!cart.isSameUser(user.getId())) {
            throw new CartException(ErrorCode.CART_ACCESS_DENIED);
        }
    }
}
