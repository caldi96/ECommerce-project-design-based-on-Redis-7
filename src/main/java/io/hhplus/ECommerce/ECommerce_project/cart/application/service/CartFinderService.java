package io.hhplus.ECommerce.ECommerce_project.cart.application.service;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartFinderService {

    private final CartRepository cartRepository;

    /**
     * 장바구니 단건 조회
     */
    public Cart getCart(Long id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new CartException(ErrorCode.CART_NOT_FOUND));
    }

    /**
     * 유저의 장바구니 전체 목록 조회
     */
    public List<Cart> findAllByUserId(Long userId) {
        return cartRepository.findAllByUser_Id(userId);
    }

    /**
     * 상품에 대한 유저 장바구니 단건 조회
     */
    public Optional<Cart> findByUserAndProduct(User user, Product product) {
        return cartRepository.findByUser_IdAndProduct_Id(user.getId(), product.getId());
    }

    /**
     * 유저의 장바구니 전체 목록 조회 (상품 포함)
     */
    public List<Cart> findAllByUserIdWithProduct(Long userId) {
        return cartRepository.findAllByUserIdWithProduct(userId);
    }

}
