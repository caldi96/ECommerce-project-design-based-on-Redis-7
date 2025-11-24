package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.service.CartFinderService;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.service.CartDomainService;
import io.hhplus.ECommerce.ECommerce_project.cart.infrastructure.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCartUseCase {

    private final CartRepository cartRepository;
    private final CartDomainService cartDomainService;
    private final CartFinderService cartFinderService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;

    @Transactional
    public void execute(Long cartId, Long userId) {

        cartDomainService.validateId(cartId);
        userDomainService.validateId(userId);

        // 1. 유저 존재 유무 확인
        User user = userFinderService.getUser(userId);

        // 2. 장바구니 존재 여부 확인
        Cart cart = cartFinderService.getCart(cartId);

        // 3. 사용자 검증 (다른 사용자의 장바구니 삭제 방지)
        cartDomainService.validateSameUser(cart, user);

        // 4. 장바구니 상품 삭제
        cartRepository.delete(cart);
    }
}
