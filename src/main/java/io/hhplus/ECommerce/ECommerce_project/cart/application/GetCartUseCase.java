package io.hhplus.ECommerce.ECommerce_project.cart.application;

import io.hhplus.ECommerce.ECommerce_project.cart.application.service.CartFinderService;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.presentation.response.GetCartResponse;
import io.hhplus.ECommerce.ECommerce_project.user.application.service.UserFinderService;
import io.hhplus.ECommerce.ECommerce_project.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCartUseCase {

    private final CartFinderService cartFinderService;
    private final UserDomainService userDomainService;
    private final UserFinderService userFinderService;

    @Transactional(readOnly = true)
    public List<GetCartResponse> execute(Long userId) {

        // 1. 유저 ID 검증
        userDomainService.validateId(userId);

        // 2. 유저 존재 유무 확인
        userFinderService.getUser(userId);

        // 3. 사용자의 장바구니 조회
        List<Cart> cartList = cartFinderService.findAllByUserIdWithProduct(userId);

        // 4. 각 장바구니 아이템마다 상품 정보 조회 후 Response 생성
        return cartList.stream()
                .map(cart -> GetCartResponse.from(cart, cart.getProduct()))
                .toList();
    }
}
